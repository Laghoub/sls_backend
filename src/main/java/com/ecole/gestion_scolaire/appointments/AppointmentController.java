package com.ecole.gestion_scolaire.appointments;

import com.ecole.gestion_scolaire.security.service.SecurityUser;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {
 private final JdbcTemplate db;
 public AppointmentController(JdbcTemplate db){this.db=db;}
 private SecurityUser user(){Object p=SecurityContextHolder.getContext().getAuthentication().getPrincipal();if(!(p instanceof SecurityUser s))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);return s;}
 private long personId(){Long id=db.queryForObject("SELECT person_id FROM user_account WHERE id=?",Long.class,user().getId());return id;}
 private long guardian(){List<Long> ids=db.query("SELECT id FROM guardian WHERE person_id=?",(r,n)->r.getLong(1),personId());if(ids.isEmpty())throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Compte parent requis");return ids.get(0);}
 private long teacher(){List<Long> ids=db.query("SELECT t.id FROM teacher t JOIN employee e ON e.id=t.employee_id WHERE e.person_id=?",(r,n)->r.getLong(1),personId());if(ids.isEmpty())throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Compte enseignant requis");return ids.get(0);}
 private boolean belongs(long guardianId,long studentId){Integer count=db.queryForObject("SELECT count(*) FROM student_guardian WHERE guardian_id=? AND student_id=? AND active=true",Integer.class,guardianId,studentId);return count!=null&&count>0;}
 public record Request(long studentId,long teacherId,long subjectId,String reason,String preferredTimes){}
 public record Decision(OffsetDateTime startsAt,OffsetDateTime endsAt,String meetingPlace,String note){}
 public record Rejection(String reason){}
 public record Summons(long studentId,long subjectId,String reason,OffsetDateTime startsAt,OffsetDateTime endsAt,String meetingPlace){}
 @GetMapping("/parent/children") @PreAuthorize("hasAuthority('PORTAIL_PARENT_ACCEDER')")
 public List<Map<String,Object>> children(){return db.queryForList("SELECT s.id,p.first_name,p.last_name FROM student s JOIN person p ON p.id=s.person_id JOIN student_guardian sg ON sg.student_id=s.id WHERE sg.guardian_id=? AND sg.active=true ORDER BY p.last_name,p.first_name",guardian());}
 @GetMapping("/parent/children/{studentId}/teachers") @PreAuthorize("hasAuthority('PORTAIL_PARENT_ACCEDER')")
 public List<Map<String,Object>> options(@PathVariable long studentId){long g=guardian();if(!belongs(g,studentId))throw new ResponseStatusException(HttpStatus.FORBIDDEN);return db.queryForList("SELECT DISTINCT ta.teacher_id,ta.subject_id,s.name AS subject_name,p.first_name,p.last_name FROM student_enrollment se JOIN teaching_assignment ta ON ta.class_group_id=se.current_class_group_id AND ta.school_year_id=se.school_year_id JOIN subject s ON s.id=ta.subject_id JOIN teacher t ON t.id=ta.teacher_id JOIN employee e ON e.id=t.employee_id JOIN person p ON p.id=e.person_id WHERE se.student_id=? AND se.exit_date IS NULL AND ta.start_date<=CURRENT_DATE AND (ta.end_date IS NULL OR ta.end_date>=CURRENT_DATE) ORDER BY s.name,p.last_name",studentId);}
 @PostMapping("/parent") @PreAuthorize("hasAuthority('PORTAIL_PARENT_ACCEDER')") @Transactional
 public Map<String,Object> request(@RequestBody Request r){long g=guardian();if(!belongs(g,r.studentId()))throw new ResponseStatusException(HttpStatus.FORBIDDEN);if(r.reason()==null||r.reason().isBlank()||r.reason().length()>1000)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Motif obligatoire (1000 caractères maximum)");boolean allowed=options(r.studentId()).stream().anyMatch(o->((Number)o.get("teacher_id")).longValue()==r.teacherId()&&((Number)o.get("subject_id")).longValue()==r.subjectId());if(!allowed)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Cet enseignant n'enseigne pas cette matière dans la classe de l'enfant");Long id=db.queryForObject("INSERT INTO parent_teacher_appointment(guardian_id,student_id,teacher_id,subject_id,reason,preferred_times) VALUES(?,?,?,?,?,?) RETURNING id",Long.class,g,r.studentId(),r.teacherId(),r.subjectId(),r.reason().trim(),r.preferredTimes());return Map.of("id",id,"status","PENDING");}
 @GetMapping("/parent") @PreAuthorize("hasAuthority('PORTAIL_PARENT_ACCEDER')")
 public List<Map<String,Object>> parent(){return db.queryForList("SELECT a.*,s.name AS subject_name FROM parent_teacher_appointment a JOIN subject s ON s.id=a.subject_id WHERE a.guardian_id=? AND (a.origin='PARENT' OR (a.origin='TEACHER' AND a.status='CONFIRMED')) ORDER BY a.created_at DESC",guardian());}
 @GetMapping("/teacher/students") @PreAuthorize("hasAuthority('PORTAIL_ENSEIGNANT_ACCEDER')")
 public List<Map<String,Object>> teacherStudents(){
  return db.queryForList("SELECT DISTINCT se.student_id,ta.subject_id,sub.name AS subject_name,sp.first_name AS student_first_name,sp.last_name AS student_last_name FROM teaching_assignment ta JOIN student_enrollment se ON se.current_class_group_id=ta.class_group_id AND se.school_year_id=ta.school_year_id JOIN student st ON st.id=se.student_id JOIN person sp ON sp.id=st.person_id JOIN subject sub ON sub.id=ta.subject_id WHERE ta.teacher_id=? AND se.exit_date IS NULL AND ta.start_date<=CURRENT_DATE AND (ta.end_date IS NULL OR ta.end_date>=CURRENT_DATE) ORDER BY sp.last_name,sp.first_name,sub.name",teacher());
 }
 @PostMapping("/teacher/summons") @PreAuthorize("hasAuthority('PORTAIL_ENSEIGNANT_ACCEDER')") @Transactional
 public Map<String,Object> summon(@RequestBody Summons r){
  long t=teacher();
  if(r.reason()==null||r.reason().isBlank()||r.reason().length()>1000)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Motif obligatoire (1000 caractères maximum)");
  if(r.startsAt()==null||r.endsAt()==null||!r.endsAt().isAfter(r.startsAt())||!r.startsAt().isAfter(OffsetDateTime.now()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Date ou durée invalide");
  boolean assigned=teacherStudents().stream().anyMatch(o->((Number)o.get("student_id")).longValue()==r.studentId()&&((Number)o.get("subject_id")).longValue()==r.subjectId());
  if(!assigned)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Élève ou matière non affecté à cet enseignant");
  List<Long> guardians=db.query("SELECT guardian_id FROM student_guardian WHERE student_id=? AND active=true ORDER BY guardian_id",(rs,n)->rs.getLong(1),r.studentId());
  if(guardians.isEmpty())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Aucun responsable légal actif");
  Long id=db.queryForObject("INSERT INTO parent_teacher_appointment(guardian_id,student_id,teacher_id,subject_id,reason,status,starts_at,ends_at,meeting_place,origin) VALUES(?,?,?,?,?,'PENDING',?,?,?,'TEACHER') RETURNING id",Long.class,guardians.get(0),r.studentId(),t,r.subjectId(),r.reason().trim(),r.startsAt(),r.endsAt(),r.meetingPlace());
  return Map.of("id",id,"status","PENDING","origin","TEACHER");
 }
 @GetMapping("/teacher/summons") @PreAuthorize("hasAuthority('PORTAIL_ENSEIGNANT_ACCEDER')")
 public List<Map<String,Object>> teacherSummons(){
  return db.queryForList("SELECT a.*,p.first_name AS student_first_name,p.last_name AS student_last_name,sub.name AS subject_name FROM parent_teacher_appointment a JOIN student st ON st.id=a.student_id JOIN person p ON p.id=st.person_id JOIN subject sub ON sub.id=a.subject_id WHERE a.teacher_id=? AND a.origin='TEACHER' ORDER BY a.created_at DESC",teacher());
 }
 @GetMapping("/teacher") @PreAuthorize("hasAuthority('PORTAIL_ENSEIGNANT_ACCEDER')")
 public List<Map<String,Object>> teacherMeetings(){return db.queryForList("SELECT a.id,a.student_id,a.subject_id,a.reason,a.status,a.starts_at,a.ends_at,a.meeting_place,a.decision_note,p.first_name AS student_first_name,p.last_name AS student_last_name FROM parent_teacher_appointment a JOIN student st ON st.id=a.student_id JOIN person p ON p.id=st.person_id WHERE a.teacher_id=? AND a.status='CONFIRMED' ORDER BY a.starts_at",teacher());}
 @GetMapping("/admin") @PreAuthorize("hasAuthority('RENDEZ_VOUS_ADMIN')")
 public List<Map<String,Object>> admin(@RequestParam(required=false) String status){if(status!=null&&!Set.of("PENDING","CONFIRMED","REJECTED","CANCELLED").contains(status))throw new ResponseStatusException(HttpStatus.BAD_REQUEST);return status==null?db.queryForList("SELECT a.*,sp.first_name AS student_first_name,sp.last_name AS student_last_name,sub.name AS subject_name FROM parent_teacher_appointment a JOIN student st ON st.id=a.student_id JOIN person sp ON sp.id=st.person_id JOIN subject sub ON sub.id=a.subject_id ORDER BY a.created_at DESC LIMIT 500"):db.queryForList("SELECT a.*,sp.first_name AS student_first_name,sp.last_name AS student_last_name,sub.name AS subject_name FROM parent_teacher_appointment a JOIN student st ON st.id=a.student_id JOIN person sp ON sp.id=st.person_id JOIN subject sub ON sub.id=a.subject_id WHERE a.status=? ORDER BY a.created_at DESC LIMIT 500",status);}
 @PostMapping("/admin/{id}/confirm") @PreAuthorize("hasAuthority('RENDEZ_VOUS_ADMIN')") @Transactional
 public Map<String,Object> confirm(@PathVariable long id,@RequestBody Decision d){if(d.startsAt()==null||d.endsAt()==null||!d.endsAt().isAfter(d.startsAt())||!d.startsAt().isAfter(OffsetDateTime.now()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Date et durée invalides");List<Map<String,Object>> rows=db.queryForList("SELECT * FROM parent_teacher_appointment WHERE id=? FOR UPDATE",id);if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND);Map<String,Object> a=rows.get(0);if(!"PENDING".equals(a.get("status")))throw new ResponseStatusException(HttpStatus.CONFLICT,"Demande déjà traitée");long teacherId=((Number)a.get("teacher_id")).longValue();Integer conflicts=db.queryForObject("SELECT count(*) FROM parent_teacher_appointment WHERE teacher_id=? AND status='CONFIRMED' AND starts_at<? AND ends_at>?",Integer.class,teacherId,d.endsAt(),d.startsAt());if(conflicts!=null&&conflicts>0)throw new ResponseStatusException(HttpStatus.CONFLICT,"Enseignant déjà occupé");db.update("UPDATE parent_teacher_appointment SET status='CONFIRMED',starts_at=?,ends_at=?,meeting_place=?,decision_note=?,decided_by=?,decided_at=now(),updated_at=now() WHERE id=?",d.startsAt(),d.endsAt(),d.meetingPlace(),d.note(),user().getId(),id);queueEmails(id,"CONFIRMED");return Map.of("id",id,"status","CONFIRMED","notifications","QUEUED");}
 @PostMapping("/admin/{id}/reject") @PreAuthorize("hasAuthority('RENDEZ_VOUS_ADMIN')") @Transactional
 public Map<String,Object> reject(@PathVariable long id,@RequestBody Rejection r){if(r.reason()==null||r.reason().isBlank())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Motif obligatoire");int n=db.update("UPDATE parent_teacher_appointment SET status='REJECTED',decision_note=?,decided_by=?,decided_at=now(),updated_at=now() WHERE id=? AND status='PENDING'",r.reason(),user().getId(),id);if(n==0)throw new ResponseStatusException(HttpStatus.CONFLICT,"Demande introuvable ou déjà traitée");queueEmails(id,"REJECTED");return Map.of("id",id,"status","REJECTED","notifications","QUEUED");}
 @PostMapping("/admin/{id}/reschedule") @PreAuthorize("hasAuthority('RENDEZ_VOUS_ADMIN')") @Transactional
 public Map<String,Object> reschedule(@PathVariable long id,@RequestBody Decision d){if(d.startsAt()==null||d.endsAt()==null||!d.endsAt().isAfter(d.startsAt())||!d.startsAt().isAfter(OffsetDateTime.now()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Date invalide");List<Map<String,Object>> rows=db.queryForList("SELECT * FROM parent_teacher_appointment WHERE id=? FOR UPDATE",id);if(rows.isEmpty()||!"CONFIRMED".equals(rows.get(0).get("status")))throw new ResponseStatusException(HttpStatus.CONFLICT);long t=((Number)rows.get(0).get("teacher_id")).longValue();Integer conflicts=db.queryForObject("SELECT count(*) FROM parent_teacher_appointment WHERE teacher_id=? AND id<>? AND status='CONFIRMED' AND starts_at<? AND ends_at>?",Integer.class,t,id,d.endsAt(),d.startsAt());if(conflicts!=null&&conflicts>0)throw new ResponseStatusException(HttpStatus.CONFLICT,"Créneau occupé");db.update("UPDATE parent_teacher_appointment SET starts_at=?,ends_at=?,meeting_place=?,decision_note=?,decided_by=?,decided_at=now(),updated_at=now() WHERE id=?",d.startsAt(),d.endsAt(),d.meetingPlace(),d.note(),user().getId(),id);queueEmails(id,"RESCHEDULED");return Map.of("id",id,"status","CONFIRMED","notifications","QUEUED");}
 @GetMapping("/admin/{id}/notifications") @PreAuthorize("hasAuthority('RENDEZ_VOUS_ADMIN')")
 public List<Map<String,Object>> notifications(@PathVariable long id){return db.queryForList("SELECT id,recipient_email,status,attempts,last_error,sent_at,created_at FROM parent_teacher_appointment_mail WHERE appointment_id=? ORDER BY id DESC",id);}
 @PostMapping("/parent/{id}/cancel") @PreAuthorize("hasAuthority('PORTAIL_PARENT_ACCEDER')") @Transactional
 public Map<String,Object> cancel(@PathVariable long id){int n=db.update("UPDATE parent_teacher_appointment SET status='CANCELLED',updated_at=now() WHERE id=? AND guardian_id=? AND origin='PARENT' AND status IN ('PENDING','CONFIRMED')",id,guardian());if(n==0)throw new ResponseStatusException(HttpStatus.CONFLICT);queueEmails(id,"CANCELLED");return Map.of("id",id,"status","CANCELLED");}
 private void queueEmails(long id,String action){
  List<Map<String,Object>> people=db.queryForList("SELECT a.starts_at,a.ends_at,a.meeting_place,a.decision_note,a.reason,a.origin, gp.email AS guardian_email,gp.first_name AS guardian_first_name, tp.email AS teacher_email,tp.first_name AS teacher_first_name,tp.last_name AS teacher_last_name,sp.first_name AS student_first_name,sp.last_name AS student_last_name,sub.name AS subject_name FROM parent_teacher_appointment a JOIN guardian g ON g.id=a.guardian_id JOIN person gp ON gp.id=g.person_id JOIN teacher t ON t.id=a.teacher_id JOIN employee e ON e.id=t.employee_id JOIN person tp ON tp.id=e.person_id JOIN student s ON s.id=a.student_id JOIN person sp ON sp.id=s.person_id JOIN subject sub ON sub.id=a.subject_id WHERE a.id=?",id);
  if(people.isEmpty())return;
  Map<String,Object> p=people.get(0);
  String title=switch(action){case "CONFIRMED"->"Rendez-vous confirmé";case "RESCHEDULED"->"Rendez-vous reporté";case "REJECTED"->"Demande de rendez-vous refusée";case "CANCELLED"->"Rendez-vous annulé";default->"Information concernant votre rendez-vous";};
  for(String key:List.of("guardian_email","teacher_email")){
   Object email=p.get(key);
   if(email instanceof String address&&!address.isBlank()){
    boolean parent=key.equals("guardian_email");
    String subject=title+" — Groupe Scolaire Sciences et Lettres";
    String body=mailLayout(p,action,title,parent);
    db.update("INSERT INTO parent_teacher_appointment_mail(appointment_id,recipient_email,subject,body) VALUES(?,?,?,?)",id,address.trim(),subject,body);
   }
  }
 }
 private static String esc(Object value){if(value==null)return "";return String.valueOf(value).replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");}
 private static String fullName(Map<String,Object> p,String prefix){return (String.valueOf(p.getOrDefault(prefix+"_first_name","")+" "+p.getOrDefault(prefix+"_last_name","")).trim());}
 private static String dateTime(Object value){
  if(value==null)return "À définir";
  try{java.time.OffsetDateTime d=value instanceof java.time.OffsetDateTime od?od:java.time.OffsetDateTime.parse(value.toString());return d.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm",java.util.Locale.FRANCE));}catch(Exception ignored){return String.valueOf(value);}
 }
 private static String row(String label,String value){return "<tr><td style='padding:9px 12px;color:#64748b;border-bottom:1px solid #e2e8f0;width:36%'>"+esc(label)+"</td><td style='padding:9px 12px;color:#172033;border-bottom:1px solid #e2e8f0;font-weight:600'>"+esc(value)+"</td></tr>";}
 private static String mailLayout(Map<String,Object> p,String action,String title,boolean parent){
  String student=fullName(p,"student"),teacher=fullName(p,"teacher");
  String greeting=parent?"Madame, Monsieur,":"Bonjour "+esc(p.get("teacher_first_name"))+",";
  boolean teacherOrigin="TEACHER".equals(p.get("origin"));
  String explanation=switch(action){
   case "CONFIRMED"->teacherOrigin?(parent?"L’enseignant de votre enfant vous convoque à un entretien, confirmé par l’administration.":"Votre convocation d’un responsable légal a été validée par l’administration."):(parent?"Votre demande de rencontre avec un enseignant a été validée par l’administration.":"Un rendez-vous avec un parent d’élève a été programmé par l’administration.");
   case "RESCHEDULED"->"La date ou l'heure de votre rendez-vous a été modifiée par l'administration.";
   case "REJECTED"->"La demande de rendez-vous n'a pas pu être acceptée. Vous trouverez les informations ci-dessous.";
   case "CANCELLED"->"Le rendez-vous a été annulé. Vous trouverez les informations ci-dessous.";
   default->"Voici les informations concernant votre rendez-vous.";
  };
  StringBuilder rows=new StringBuilder();
  rows.append(row("Élève",student)).append(row("Enseignant",teacher)).append(row("Matière",String.valueOf(p.getOrDefault("subject_name","")))).append(row("Motif de la demande",String.valueOf(p.getOrDefault("reason",""))));
  if(!"REJECTED".equals(action)){
   rows.append(row("Début",dateTime(p.get("starts_at")))).append(row("Fin",dateTime(p.get("ends_at"))));
   if(p.get("meeting_place")!=null&&!p.get("meeting_place").toString().isBlank())rows.append(row("Lieu",p.get("meeting_place").toString()));
  }
  if(p.get("decision_note")!=null&&!p.get("decision_note").toString().isBlank())rows.append(row("Observation de l'administration",p.get("decision_note").toString()));
  return "<!doctype html><html lang='fr'><head><meta charset='UTF-8'></head><body style='margin:0;background:#f4f7fb;font-family:Arial,sans-serif;color:#172033'>"+
   "<div style='max-width:680px;margin:24px auto;background:#fff;border:1px solid #e2e8f0;border-radius:16px;overflow:hidden'>"+
   "<div style='background:#0f2747;padding:22px;text-align:center'><img src='cid:slsLogo' alt='' style='height:70px;max-width:140px;object-fit:contain'><div style='color:#fff;font-size:20px;font-weight:700'>Groupe Scolaire Sciences et Lettres</div><div style='color:#d8bc75;font-size:13px;margin-top:5px'>المجمع المدرسي الخاص علوم وآداب</div></div>"+
   "<div style='padding:28px'><h2 style='color:#0f2747;margin-top:0'>"+esc(title)+"</h2><p>"+greeting+"</p><p style='line-height:1.7'>"+esc(explanation)+"</p>"+
   "<div style='background:#f8fafc;border:1px solid #e2e8f0;border-radius:12px;overflow:hidden'><div style='background:#f0e7d1;color:#0f2747;padding:12px 16px;font-weight:bold'>Détails du rendez-vous</div><table style='border-collapse:collapse;width:100%;font-size:14px'>"+rows+"</table></div>"+
   "<p style='margin-top:22px;line-height:1.7'>Pour toute précision, veuillez contacter l'administration de l'établissement.</p>"+
   "<p style='margin-top:28px;color:#64748b;font-size:12px'>Message automatique envoyé par le système de gestion scolaire.</p></div></div></body></html>";
 }
}
