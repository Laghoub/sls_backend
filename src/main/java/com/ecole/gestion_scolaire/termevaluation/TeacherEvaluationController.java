package com.ecole.gestion_scolaire.termevaluation;
import jakarta.validation.Valid;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;

@RestController
@RequestMapping("/api/term-evaluations/teacher")
public class TeacherEvaluationController {
 private final TeacherEvaluationService service;
 public TeacherEvaluationController(TeacherEvaluationService service){this.service=service;}
 @GetMapping("/campaigns")
 @PreAuthorize("hasAuthority('EVALUATION_TRIMESTRIELLE_SAISIR')")
 public List<Map<String,Object>> campaigns(){return service.campaigns();}
 @GetMapping("/students")
 @PreAuthorize("hasAuthority('EVALUATION_TRIMESTRIELLE_SAISIR')")
 public List<Map<String,Object>> students(@RequestParam Long campaignId,@RequestParam Long classGroupId,@RequestParam Long subjectId){
  return service.students(campaignId,classGroupId,subjectId);
 }
 @GetMapping("/evaluations/{id}")
 @PreAuthorize("hasAuthority('EVALUATION_TRIMESTRIELLE_SAISIR')")
 public Map<String,Object> evaluation(@PathVariable Long id){return service.evaluation(id);}
 @PostMapping("/draft")
 @PreAuthorize("hasAuthority('EVALUATION_TRIMESTRIELLE_SAISIR')")
 public Map<String,Object> save(@Valid @RequestBody TeacherEvaluationRequest request){return service.save(request);}
 @PostMapping("/{id}/submit")
 @PreAuthorize("hasAuthority('EVALUATION_TRIMESTRIELLE_SOUMETTRE')")
 public Map<String,Object> submit(@PathVariable Long id){return service.submit(id);}
}

@Service
class TeacherEvaluationService {
 private final NamedParameterJdbcTemplate db;
 TeacherEvaluationService(NamedParameterJdbcTemplate db){this.db=db;}
 private ResponseStatusException forbidden(){return new ResponseStatusException(HttpStatus.FORBIDDEN,"Affectation enseignant non autorisée");}
 private Long teacherId(){
  var a=SecurityContextHolder.getContext().getAuthentication();
  if(a==null || !a.isAuthenticated())throw forbidden();
  var ids=db.queryForList("""
   SELECT t.id FROM teacher t JOIN employee e ON e.id=t.employee_id
   JOIN user_account u ON u.person_id=e.person_id
   WHERE u.username=:username
   """,Map.of("username",a.getName()),Long.class);
  if(ids.size()!=1)throw forbidden();
  return ids.get(0);
 }
 private boolean allowed(Long campaign,Long cls,Long subject,Long teacher,Long student){
  var p=new MapSqlParameterSource().addValue("campaign",campaign).addValue("cls",cls)
   .addValue("subject",subject).addValue("teacher",teacher).addValue("student",student);
  return Boolean.TRUE.equals(db.queryForObject("""
   SELECT EXISTS(
    SELECT 1 FROM term_evaluation_campaign c
    JOIN teaching_assignment a ON a.school_year_id=c.school_year_id
     AND a.class_group_id=:cls AND a.subject_id=:subject AND a.teacher_id=:teacher
     AND a.status='ACTIVE'
     AND a.start_date<=c.evaluation_to AND (a.end_date IS NULL OR a.end_date>=c.evaluation_from)
    JOIN student_enrollment e ON e.school_year_id=c.school_year_id
     AND e.current_class_group_id=:cls AND e.student_id=:student AND e.status='ACTIVE'
    JOIN class_group cg ON cg.id=:cls
    JOIN level lvl ON lvl.id=cg.level_id
    JOIN cycle cyc ON cyc.id=lvl.cycle_id
    WHERE c.id=:campaign AND c.school_year_id=cg.school_year_id
    AND (c.cycle_code IS NULL OR c.cycle_code=cyc.code)
    AND (NOT EXISTS(SELECT 1 FROM term_evaluation_campaign_class cc WHERE cc.campaign_id=c.id)
     OR EXISTS(SELECT 1 FROM term_evaluation_campaign_class cc WHERE cc.campaign_id=c.id AND cc.class_group_id=:cls))
   )
   """,p,Boolean.class));
 }
 private boolean open(Long campaign){
  return Boolean.TRUE.equals(db.queryForObject("""
   SELECT EXISTS(SELECT 1 FROM term_evaluation_campaign WHERE id=:id
    AND status IN ('SCHEDULED','OPEN') AND now()>=opens_at AND now()<closes_at)
   """,Map.of("id",campaign),Boolean.class));
 }
 @Transactional(readOnly=true)
 public List<Map<String,Object>> campaigns(){
  var p=Map.of("teacher",teacherId());
  return db.queryForList("""
   SELECT DISTINCT c.* FROM term_evaluation_campaign c
   JOIN teaching_assignment a ON a.school_year_id=c.school_year_id AND a.teacher_id=:teacher
   AND a.status='ACTIVE' AND a.start_date<=c.evaluation_to
   AND (a.end_date IS NULL OR a.end_date>=c.evaluation_from)
   WHERE c.status IN ('SCHEDULED','OPEN') AND now() BETWEEN c.opens_at AND c.closes_at
   ORDER BY c.opens_at DESC
   """,p);
 }
 @Transactional(readOnly=true)
 public List<Map<String,Object>> students(Long campaignId,Long classGroupId,Long subjectId){
  Long teacher=teacherId();
  // Vérification de l'affectation avant de retourner la liste.
  var p=new MapSqlParameterSource().addValue("campaign",campaignId)
   .addValue("cls",classGroupId).addValue("subject",subjectId).addValue("teacher",teacher);
  if(!Boolean.TRUE.equals(db.queryForObject("""
   SELECT EXISTS(SELECT 1 FROM term_evaluation_campaign c
    JOIN teaching_assignment a ON a.school_year_id=c.school_year_id
    AND a.teacher_id=:teacher AND a.class_group_id=:cls AND a.subject_id=:subject
    AND a.status='ACTIVE' AND a.start_date<=c.evaluation_to
    AND (a.end_date IS NULL OR a.end_date>=c.evaluation_from)
    WHERE c.id=:campaign AND c.school_year_id=(SELECT school_year_id FROM class_group WHERE id=:cls)
     AND (c.cycle_code IS NULL OR c.cycle_code=(SELECT cyc.code FROM class_group cg
       JOIN level lvl ON lvl.id=cg.level_id JOIN cycle cyc ON cyc.id=lvl.cycle_id WHERE cg.id=:cls))
     AND (NOT EXISTS(SELECT 1 FROM term_evaluation_campaign_class cc WHERE cc.campaign_id=c.id)
       OR EXISTS(SELECT 1 FROM term_evaluation_campaign_class cc WHERE cc.campaign_id=c.id AND cc.class_group_id=:cls)))
   """,p,Boolean.class)))throw forbidden();
  return db.queryForList("""
   SELECT s.id AS student_id,p.first_name,p.last_name,
    ev.id AS evaluation_id,ev.status AS evaluation_status
   FROM student_enrollment e JOIN student s ON s.id=e.student_id
   JOIN person p ON p.id=s.person_id
   JOIN term_evaluation_campaign c ON c.school_year_id=e.school_year_id
   LEFT JOIN term_student_evaluation ev ON ev.campaign_id=c.id AND ev.student_id=s.id
    AND ev.teacher_id=:teacher AND ev.class_group_id=:cls AND ev.subject_id=:subject
   WHERE c.id=:campaign AND e.current_class_group_id=:cls AND e.status='ACTIVE'
   ORDER BY p.last_name,p.first_name
   """,p);
 }
 @Transactional(readOnly=true)
 public Map<String,Object> evaluation(Long id){
  Long teacher=teacherId();
  var rows=db.queryForList("""
   SELECT * FROM term_student_evaluation WHERE id=:id AND teacher_id=:teacher
   """,Map.of("id",id,"teacher",teacher));
  if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Évaluation introuvable");
  var result=new LinkedHashMap<String,Object>(rows.get(0));
  result.put("answers",db.queryForList("""
   SELECT criterion_id,option_id,free_text FROM term_evaluation_answer
   WHERE evaluation_id=:id ORDER BY criterion_id
   """,Map.of("id",id)));
  return result;
 }
 @Transactional
 public Map<String,Object> save(TeacherEvaluationRequest r){
  Long teacher=teacherId();
  if(!open(r.campaignId()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Campagne fermée");
  if(!allowed(r.campaignId(),r.classGroupId(),r.subjectId(),teacher,r.studentId()))throw forbidden();
  var p=new MapSqlParameterSource().addValue("campaign",r.campaignId())
   .addValue("student",r.studentId()).addValue("teacher",teacher)
   .addValue("cls",r.classGroupId()).addValue("subject",r.subjectId())
   .addValue("obs",r.observation()).addValue("diff",r.difficulties()).addValue("reco",r.recommendations());
  var saved=db.queryForList("""
   INSERT INTO term_student_evaluation(campaign_id,student_id,teacher_id,class_group_id,subject_id,
    observation,difficulties,recommendations)
   VALUES(:campaign,:student,:teacher,:cls,:subject,:obs,:diff,:reco)
   ON CONFLICT(campaign_id,student_id,teacher_id,class_group_id,subject_id)
   DO UPDATE SET observation=EXCLUDED.observation,difficulties=EXCLUDED.difficulties,
    recommendations=EXCLUDED.recommendations,updated_at=now()
   WHERE term_student_evaluation.status IN ('DRAFT','RETURNED')
   RETURNING id,status
   """,p);
  if(saved.isEmpty())throw new ResponseStatusException(HttpStatus.CONFLICT,"Évaluation déjà soumise");
  Long id=((Number)saved.get(0).get("id")).longValue();
  if(r.answers()!=null)for(var answer:r.answers()){
   var ap=new MapSqlParameterSource().addValue("id",id).addValue("criterion",answer.criterionId())
    .addValue("option",answer.optionId()).addValue("free",answer.freeText());
   // L'option doit appartenir au critère, et le critère doit être actif.
   int updated=db.update("""
    INSERT INTO term_evaluation_answer(evaluation_id,criterion_id,option_id,free_text)
    SELECT :id,cr.id,:option,:free FROM term_evaluation_criterion cr
    WHERE cr.id=:criterion AND cr.active=TRUE
     AND (:option IS NULL OR EXISTS(SELECT 1 FROM term_evaluation_option o
      WHERE o.id=:option AND o.criterion_id=cr.id))
    ON CONFLICT(evaluation_id,criterion_id) DO UPDATE
    SET option_id=EXCLUDED.option_id,free_text=EXCLUDED.free_text
    """,ap);
   if(updated!=1)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Critère ou option invalide");
  }
  return saved.get(0);
 }
 @Transactional
 public Map<String,Object> submit(Long id){
  Long teacher=teacherId();
  var rows=db.queryForList("""
   UPDATE term_student_evaluation e SET status='SUBMITTED',submitted_at=now(),updated_at=now()
   WHERE e.id=:id AND e.teacher_id=:teacher AND e.status IN ('DRAFT','RETURNED')
    AND EXISTS(SELECT 1 FROM term_evaluation_campaign c WHERE c.id=e.campaign_id
     AND c.status IN ('SCHEDULED','OPEN') AND now()>=c.opens_at AND now()<c.closes_at)
   RETURNING e.id,e.status,e.submitted_at
   """,Map.of("id",id,"teacher",teacher));
  if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.CONFLICT,"Soumission non autorisée");
  return rows.get(0);
 }
}
