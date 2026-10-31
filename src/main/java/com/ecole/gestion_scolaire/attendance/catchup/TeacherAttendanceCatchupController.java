package com.ecole.gestion_scolaire.attendance.catchup;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Date;
import java.time.*;
import java.util.*;

/** Rattrapage historique : mêmes tables que l'assiduité quotidienne, validation séparée. */
@RestController
@RequestMapping("/api/teacher-attendance/catchup")
public class TeacherAttendanceCatchupController {
    private final JdbcTemplate db;
    public TeacherAttendanceCatchupController(JdbcTemplate db){this.db=db;}

    public record Line(@NotNull Long teacherId,@NotNull Long classGroupId,@NotNull Long subjectId,
                       @NotNull Long timeSlotId,@NotNull LocalDate date,
                       @NotBlank String status,Integer lateMinutes,String reason,String notes){}
    public record Batch(@NotEmpty @Size(max=150) List<@Valid Line> lines){}
    public record Pattern(@NotNull Long teacherId,@NotNull Long classGroupId,@NotNull Long subjectId,
                          @NotNull Long timeSlotId,@Min(1) @Max(7) int dayOfWeek){}
    public record Template(@NotBlank @Size(max=120) String name,@NotNull LocalDate fromDate,
                           @NotNull LocalDate toDate,@NotEmpty @Size(max=100) List<@Valid Pattern> patterns){}
    public record Generate(@NotNull LocalDate fromDate,@NotNull LocalDate toDate,boolean excludeHolidays){}

    private long actor(){
        String name=SecurityContextHolder.getContext().getAuthentication().getName();
        List<Long> ids=db.query("SELECT id FROM user_account WHERE username=?",(rs,i)->rs.getLong(1),name);
        if(ids.isEmpty())throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Utilisateur inconnu");
        return ids.get(0);
    }
    private void ensure(boolean condition,String message){if(!condition)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
    private boolean exists(String sql,Object...args){return Boolean.TRUE.equals(db.queryForObject("SELECT EXISTS("+sql+")",Boolean.class,args));}
    private void verify(Line l){
        ensure(!l.date().isAfter(LocalDate.now()),"Saisie historique : date future interdite");
        ensure(Set.of("PRESENT","ABSENT","RETARD").contains(l.status()),"Statut invalide");
        ensure(l.status().equals("RETARD") ? l.lateMinutes()!=null&&l.lateMinutes()>0 : l.lateMinutes()==null||l.lateMinutes()==0,"Minutes de retard invalides");
        ensure(exists("SELECT 1 FROM teacher WHERE id=?",l.teacherId()),"Enseignant introuvable");
        ensure(exists("SELECT 1 FROM class_group WHERE id=?",l.classGroupId()),"Classe introuvable");
        ensure(exists("SELECT 1 FROM subject WHERE id=?",l.subjectId()),"Matière introuvable");
        ensure(exists("SELECT 1 FROM time_slot WHERE id=? AND start_time<end_time",l.timeSlotId()),"Créneau invalide");
    }
    private void checkCollision(Line l){
        // Verrouillage par enseignant + date pour sérialiser les insertions concurrentes.
        db.query("SELECT pg_advisory_xact_lock(?)",rs -> { },l.teacherId()*100000000L+l.date().toEpochDay());
        boolean collision=exists("SELECT 1 FROM class_session cs JOIN time_slot ts ON ts.id=cs.time_slot_id " +
          "JOIN time_slot proposed ON proposed.id=? WHERE cs.teacher_id=? AND cs.session_date=? " +
          "AND ts.start_time<proposed.end_time AND proposed.start_time<ts.end_time",
          l.timeSlotId(),l.teacherId(),Date.valueOf(l.date()));
        if(collision)throw new ResponseStatusException(HttpStatus.CONFLICT,"Une séance existe déjà sur ce créneau pour cet enseignant");
    }
    private long create(Line l,long user){
        verify(l);checkCollision(l);
        Long session=db.queryForObject("INSERT INTO class_session(session_date,teacher_id,class_group_id,subject_id,time_slot_id,status,generated_from_schedule,created_at,updated_at) " +
            "VALUES(?,?,?,?,?,'PLANNED',false,now(),now()) RETURNING id",Long.class,
            Date.valueOf(l.date()),l.teacherId(),l.classGroupId(),l.subjectId(),l.timeSlotId());
        Long attendance=db.queryForObject("INSERT INTO teacher_attendance(class_session_id,teacher_id,attendance_status,late_minutes,reason,validation_status,recorded_by,recorded_at,notes) " +
            "VALUES(?,?,?,?,?,'PENDING',?,now(),?) RETURNING id",Long.class,
            session,l.teacherId(),l.status(),l.status().equals("RETARD")?l.lateMinutes():0,l.reason(),user,l.notes());
        db.update("INSERT INTO teacher_attendance_catchup(teacher_attendance_id,created_by) VALUES(?,?)",attendance,user);
        return attendance;
    }
    @PostMapping("/batch") @PreAuthorize("hasAuthority('ASSIDUITE_ENSEIGNANT_SAISIR')") @Transactional
    public Map<String,Object> batch(@Valid @RequestBody Batch batch){
        long user=actor();List<Long> ids=new ArrayList<>();
        for(Line l:batch.lines())ids.add(create(l,user));
        return Map.of("created",ids.size(),"attendanceIds",ids,"validationStatus","PENDING");
    }
    @GetMapping @PreAuthorize("hasAuthority('ASSIDUITE_ENSEIGNANT_CONSULTER')")
    public List<Map<String,Object>> list(@RequestParam LocalDate from,@RequestParam LocalDate to,
                                         @RequestParam(required=false) Long teacherId){
        ensure(!to.isBefore(from)&&to.toEpochDay()-from.toEpochDay()<=366,"Période invalide");
        String sql="SELECT ta.id,cs.session_date,ta.teacher_id,cs.class_group_id,cs.subject_id,cs.time_slot_id,"+
            "ts.start_time,ts.end_time,ta.attendance_status,ta.validation_status,ta.late_minutes,ta.reason,ta.notes,"+
            "c.created_at FROM teacher_attendance_catchup c JOIN teacher_attendance ta ON ta.id=c.teacher_attendance_id " +
            "JOIN class_session cs ON cs.id=ta.class_session_id JOIN time_slot ts ON ts.id=cs.time_slot_id " +
            "WHERE cs.session_date BETWEEN ? AND ? ";
        if(teacherId!=null){
            return db.queryForList(sql+"AND ta.teacher_id=? ORDER BY cs.session_date,ts.start_time",
                Date.valueOf(from),Date.valueOf(to),teacherId);
        }
        return db.queryForList(sql+"ORDER BY cs.session_date,ts.start_time",
            Date.valueOf(from),Date.valueOf(to));
    }
    @PatchMapping("/{id}/validate") @PreAuthorize("hasAuthority('ASSIDUITE_ENSEIGNANT_VALIDER')") @Transactional
    public Map<String,Object> validate(@PathVariable long id){return decision(id,"VALIDATED");}
    @PatchMapping("/{id}/reject") @PreAuthorize("hasAuthority('ASSIDUITE_ENSEIGNANT_VALIDER')") @Transactional
    public Map<String,Object> reject(@PathVariable long id){return decision(id,"REJECTED");}
    private Map<String,Object> decision(long id,String status){
        int n=db.update("UPDATE teacher_attendance SET validation_status=?,validated_by=?,validated_at=now() " +
            "WHERE id=? AND validation_status='PENDING' AND EXISTS (SELECT 1 FROM teacher_attendance_catchup WHERE teacher_attendance_id=?)",
            status,actor(),id,id);
        if(n!=1)throw new ResponseStatusException(HttpStatus.CONFLICT,"Saisie inexistante ou déjà traitée");
        return Map.of("id",id,"validationStatus",status);
    }
    @PostMapping("/templates") @PreAuthorize("hasAuthority('ASSIDUITE_ENSEIGNANT_SAISIR')") @Transactional
    public Map<String,Object> template(@Valid @RequestBody Template request){
        ensure(!request.toDate().isBefore(request.fromDate())&&request.toDate().toEpochDay()-request.fromDate().toEpochDay()<=366,"Période invalide");
        long actor=actor();
        Long id=db.queryForObject("INSERT INTO teacher_attendance_catchup_template(name,from_date,to_date,created_by) VALUES(?,?,?,?) RETURNING id",Long.class,
            request.name(),Date.valueOf(request.fromDate()),Date.valueOf(request.toDate()),actor);
        for(Pattern p:request.patterns()){
            ensure(exists("SELECT 1 FROM teacher WHERE id=?",p.teacherId())&&exists("SELECT 1 FROM class_group WHERE id=?",p.classGroupId())&&
                   exists("SELECT 1 FROM subject WHERE id=?",p.subjectId())&&exists("SELECT 1 FROM time_slot WHERE id=?",p.timeSlotId()),"Référence invalide");
            db.update("INSERT INTO teacher_attendance_catchup_pattern(template_id,teacher_id,class_group_id,subject_id,time_slot_id,day_of_week) VALUES(?,?,?,?,?,?)",
                id,p.teacherId(),p.classGroupId(),p.subjectId(),p.timeSlotId(),p.dayOfWeek());
        }
        return Map.of("id",id,"patterns",request.patterns().size());
    }
    @GetMapping("/templates") @PreAuthorize("hasAuthority('ASSIDUITE_ENSEIGNANT_CONSULTER')")
    public List<Map<String,Object>> templates(){return db.queryForList("SELECT * FROM teacher_attendance_catchup_template ORDER BY id DESC LIMIT 100");}
    @PostMapping("/templates/{id}/generate") @PreAuthorize("hasAuthority('ASSIDUITE_ENSEIGNANT_SAISIR')") @Transactional
    public Map<String,Object> generate(@PathVariable long id,@Valid @RequestBody Generate request){
        ensure(!request.toDate().isBefore(request.fromDate())&&request.toDate().toEpochDay()-request.fromDate().toEpochDay()<=93,"Période limitée à 93 jours");
        List<Map<String,Object>> templates=db.queryForList("SELECT * FROM teacher_attendance_catchup_template WHERE id=?",id);
        if(templates.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Planning historique introuvable");
        Map<String,Object> t=templates.get(0);
        LocalDate begin=((Date)t.get("from_date")).toLocalDate(),end=((Date)t.get("to_date")).toLocalDate();
        ensure(!request.fromDate().isBefore(begin)&&!request.toDate().isAfter(end)&&!request.toDate().isAfter(LocalDate.now()),"Période hors planning historique");
        List<Map<String,Object>> patterns=db.queryForList("SELECT * FROM teacher_attendance_catchup_pattern WHERE template_id=?",id);
        List<Long> ids=new ArrayList<>();int skipped=0;long user=actor();
        for(LocalDate d=request.fromDate();!d.isAfter(request.toDate());d=d.plusDays(1)){
            if(d.getDayOfWeek()==DayOfWeek.FRIDAY||d.getDayOfWeek()==DayOfWeek.SATURDAY)continue;
            if(request.excludeHolidays()&&exists("SELECT 1 FROM teacher_attendance_catchup_excluded_date WHERE day=?",Date.valueOf(d)))continue;
            for(Map<String,Object> p:patterns){
                if(((Number)p.get("day_of_week")).intValue()!=d.getDayOfWeek().getValue())continue;
                Line l=new Line(((Number)p.get("teacher_id")).longValue(),((Number)p.get("class_group_id")).longValue(),
                    ((Number)p.get("subject_id")).longValue(),((Number)p.get("time_slot_id")).longValue(),d,"PRESENT",0,null,"Généré depuis planning historique : présence à confirmer");
                verify(l);
                // On ne crée pas une présence fictive validée : statut PENDING jusqu'à confirmation explicite.
                try {ids.add(create(l,user));} catch(ResponseStatusException e){if(e.getStatusCode().value()==409)skipped++;else throw e;}
            }
        }
        return Map.of("created",ids.size(),"skippedConflicts",skipped,"attendanceIds",ids,"validationStatus","PENDING");
    }
}
