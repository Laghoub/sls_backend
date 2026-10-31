package com.ecole.gestion_scolaire.assessmentplanning;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.sql.Date;
import java.time.*;
import java.util.*;

@RestController
@RequestMapping("/api/assessment-planning")
public class AssessmentScheduleController {
 private final JdbcTemplate db;
 private final AssessmentSchedulePdf pdf;
 public AssessmentScheduleController(JdbcTemplate db,AssessmentSchedulePdf pdf){this.db=db;this.pdf=pdf;}
 public record ScheduleRequest(@NotNull Long schoolYearId,@NotNull Long classGroupId,@NotBlank String assessmentType,@Min(1) @Max(3) int term,@NotBlank String title){}
 public record EntryRequest(@NotNull Long subjectId,@NotNull LocalDate assessmentDate,@NotNull LocalTime startTime,@NotNull LocalTime endTime,String room,String instructions){}
 private void require(boolean ok,String message){if(!ok)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
 private boolean exists(String query,Object...args){return Boolean.TRUE.equals(db.queryForObject("SELECT EXISTS("+query+")",Boolean.class,args));}
 private long actor(){String name=SecurityContextHolder.getContext().getAuthentication().getName();List<Long> rows=db.query("SELECT id FROM user_account WHERE username=?",(r,i)->r.getLong(1),name);if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);return rows.get(0);}
 private Map<String,Object> schedule(long id){List<Map<String,Object>> rows=db.queryForList("SELECT s.*,cg.name AS class_name,sy.label AS school_year FROM assessment_schedule s JOIN class_group cg ON cg.id=s.class_group_id JOIN school_year sy ON sy.id=s.school_year_id WHERE s.id=?",id);if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Calendrier introuvable");return rows.get(0);}
 private void draft(long id){require("DRAFT".equals(schedule(id).get("status")),"Seul un brouillon est modifiable");}
 private void validSchedule(ScheduleRequest r){require(Set.of("DEVOIR","EXAMEN").contains(r.assessmentType()),"Type invalide");require(exists("SELECT 1 FROM class_group WHERE id=? AND school_year_id=?",r.classGroupId(),r.schoolYearId()),"Classe et année incompatibles");}
 @PostMapping @PreAuthorize("hasAnyAuthority('PLANNING_EVALUATION_CREER','ROLE_ADMIN')") @Transactional
 public Map<String,Object> create(@Valid @RequestBody ScheduleRequest r){validSchedule(r);Long id=db.queryForObject("INSERT INTO assessment_schedule(school_year_id,class_group_id,assessment_type,term,title,created_by) VALUES(?,?,?,?,?,?) RETURNING id",Long.class,r.schoolYearId(),r.classGroupId(),r.assessmentType(),r.term(),r.title(),actor());return schedule(id);}
 @GetMapping @PreAuthorize("hasAnyAuthority('PLANNING_EVALUATION_CONSULTER','ROLE_ADMIN')")
 public List<Map<String,Object>> list(@RequestParam(required=false) Long classGroupId,@RequestParam(required=false) Long schoolYearId){String sql="SELECT s.*,cg.name AS class_name,sy.label AS school_year FROM assessment_schedule s JOIN class_group cg ON cg.id=s.class_group_id JOIN school_year sy ON sy.id=s.school_year_id WHERE 1=1";List<Object> args=new ArrayList<>();if(classGroupId!=null){sql+=" AND s.class_group_id=?";args.add(classGroupId);}if(schoolYearId!=null){sql+=" AND s.school_year_id=?";args.add(schoolYearId);}return db.queryForList(sql+" ORDER BY s.created_at DESC",args.toArray());}
 @GetMapping("/{id}/subjects") @PreAuthorize("hasAnyAuthority('PLANNING_EVALUATION_CONSULTER','PLANNING_EVALUATION_MODIFIER','ROLE_ADMIN')")
 public List<Map<String,Object>> availableSubjects(@PathVariable long id){
  Map<String,Object> s=schedule(id);
  return db.queryForList("SELECT DISTINCT su.id,su.name FROM subject su WHERE EXISTS (SELECT 1 FROM class_group cg WHERE cg.id=? AND (EXISTS (SELECT 1 FROM level_subject ls WHERE ls.level_id=cg.level_id AND ls.school_year_id=cg.school_year_id AND ls.subject_id=su.id AND ls.active=TRUE) OR EXISTS (SELECT 1 FROM teaching_assignment ta WHERE ta.class_group_id=cg.id AND ta.school_year_id=cg.school_year_id AND ta.subject_id=su.id))) ORDER BY su.name",s.get("class_group_id"));
 }
 @GetMapping("/{id}") @PreAuthorize("hasAnyAuthority('PLANNING_EVALUATION_CONSULTER','ROLE_ADMIN')")
 public Map<String,Object> detail(@PathVariable long id){Map<String,Object> result=new LinkedHashMap<>(schedule(id));result.put("entries",entries(id));return result;}
 private List<Map<String,Object>> entries(long id){return db.queryForList("SELECT e.*,su.name AS subject_name FROM assessment_schedule_entry e JOIN subject su ON su.id=e.subject_id WHERE e.schedule_id=? ORDER BY e.assessment_date,e.start_time",id);}
 @PutMapping("/{id}") @PreAuthorize("hasAnyAuthority('PLANNING_EVALUATION_MODIFIER','ROLE_ADMIN')") @Transactional
 public Map<String,Object> update(@PathVariable long id,@Valid @RequestBody ScheduleRequest r){draft(id);validSchedule(r);db.update("UPDATE assessment_schedule SET school_year_id=?,class_group_id=?,assessment_type=?,term=?,title=?,updated_at=now() WHERE id=?",r.schoolYearId(),r.classGroupId(),r.assessmentType(),r.term(),r.title(),id);return schedule(id);}
 private void validateEntry(long id,EntryRequest r,Long except){Map<String,Object> s=schedule(id);require(r.startTime().isBefore(r.endTime()),"Horaire invalide");require(exists("SELECT 1 FROM school_year WHERE id=? AND start_date<=? AND end_date>=?",s.get("school_year_id"),Date.valueOf(r.assessmentDate()),Date.valueOf(r.assessmentDate())),"Date hors année scolaire");require(exists("SELECT 1 FROM subject WHERE id=?",r.subjectId()),"Matière inexistante");require(exists("SELECT 1 FROM class_group cg WHERE cg.id=? AND (EXISTS (SELECT 1 FROM level_subject ls WHERE ls.level_id=cg.level_id AND ls.school_year_id=cg.school_year_id AND ls.subject_id=? AND ls.active=TRUE) OR EXISTS (SELECT 1 FROM teaching_assignment ta WHERE ta.class_group_id=cg.id AND ta.school_year_id=cg.school_year_id AND ta.subject_id=?))",s.get("class_group_id"),r.subjectId(),r.subjectId()),"Matière non configurée pour cette classe et cette année scolaire");String sql="SELECT 1 FROM assessment_schedule_entry e JOIN assessment_schedule s ON s.id=e.schedule_id WHERE s.class_group_id=? AND s.school_year_id=? AND e.assessment_date=? AND e.start_time<? AND e.end_time>?";List<Object> args=new ArrayList<>(List.of(s.get("class_group_id"),s.get("school_year_id"),Date.valueOf(r.assessmentDate()),r.endTime(),r.startTime()));if(except!=null){sql+=" AND e.id<>?";args.add(except);}if(exists(sql,args.toArray()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Chevauchement d'évaluations pour cette classe");}
 @PostMapping("/{id}/entries") @PreAuthorize("hasAnyAuthority('PLANNING_EVALUATION_MODIFIER','ROLE_ADMIN')") @Transactional
 public Map<String,Object> addEntry(@PathVariable long id,@Valid @RequestBody EntryRequest r){draft(id);validateEntry(id,r,null);Long entry=db.queryForObject("INSERT INTO assessment_schedule_entry(schedule_id,subject_id,assessment_date,start_time,end_time,room,instructions) VALUES(?,?,?,?,?,?,?) RETURNING id",Long.class,id,r.subjectId(),Date.valueOf(r.assessmentDate()),r.startTime(),r.endTime(),r.room(),r.instructions());return Map.of("id",entry);}
 @PutMapping("/{id}/entries/{entryId}") @PreAuthorize("hasAnyAuthority('PLANNING_EVALUATION_MODIFIER','ROLE_ADMIN')") @Transactional
 public Map<String,Object> editEntry(@PathVariable long id,@PathVariable long entryId,@Valid @RequestBody EntryRequest r){draft(id);require(exists("SELECT 1 FROM assessment_schedule_entry WHERE id=? AND schedule_id=?",entryId,id),"Séance introuvable");validateEntry(id,r,entryId);db.update("UPDATE assessment_schedule_entry SET subject_id=?,assessment_date=?,start_time=?,end_time=?,room=?,instructions=?,updated_at=now() WHERE id=?",r.subjectId(),Date.valueOf(r.assessmentDate()),r.startTime(),r.endTime(),r.room(),r.instructions(),entryId);return Map.of("id",entryId);}
 @DeleteMapping("/{id}/entries/{entryId}") @PreAuthorize("hasAnyAuthority('PLANNING_EVALUATION_MODIFIER','ROLE_ADMIN')") @Transactional
 public void deleteEntry(@PathVariable long id,@PathVariable long entryId){draft(id);db.update("DELETE FROM assessment_schedule_entry WHERE schedule_id=? AND id=?",id,entryId);}
 @PatchMapping("/{id}/publish") @PreAuthorize("hasAnyAuthority('PLANNING_EVALUATION_PUBLIER','ROLE_ADMIN')") @Transactional
 public Map<String,Object> publish(@PathVariable long id){draft(id);require(exists("SELECT 1 FROM assessment_schedule_entry WHERE schedule_id=?",id),"Calendrier vide");db.update("UPDATE assessment_schedule SET status='PUBLISHED',published_at=now(),updated_at=now() WHERE id=?",id);return schedule(id);}
 @PatchMapping("/{id}/archive") @PreAuthorize("hasAnyAuthority('PLANNING_EVALUATION_PUBLIER','ROLE_ADMIN')") @Transactional
 public Map<String,Object> archive(@PathVariable long id){require("PUBLISHED".equals(schedule(id).get("status")),"Calendrier non publié");db.update("UPDATE assessment_schedule SET status='ARCHIVED',updated_at=now() WHERE id=?",id);return schedule(id);}
 @DeleteMapping("/{id}") @PreAuthorize("hasAnyAuthority('PLANNING_EVALUATION_SUPPRIMER','ROLE_ADMIN')") @Transactional
 public void delete(@PathVariable long id){draft(id);db.update("DELETE FROM assessment_schedule WHERE id=?",id);}
 @GetMapping("/{id}/print") @PreAuthorize("hasAnyAuthority('PLANNING_EVALUATION_CONSULTER','ROLE_ADMIN')")
 public ResponseEntity<byte[]> print(@PathVariable long id){try{byte[] bytes=pdf.render(schedule(id),entries(id));return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).header(HttpHeaders.CONTENT_DISPOSITION,"inline; filename=planning-evaluations-"+id+".pdf").body(bytes);}catch(java.io.IOException e){throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"Erreur de génération PDF",e);}}
}
