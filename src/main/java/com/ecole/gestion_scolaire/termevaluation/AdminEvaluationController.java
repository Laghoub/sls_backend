package com.ecole.gestion_scolaire.termevaluation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@RestController
@RequestMapping("/api/term-evaluations/admin")
@PreAuthorize("hasAuthority('EVALUATION_TRIMESTRIELLE_CONTROLER')")
public class AdminEvaluationController {
 private final NamedParameterJdbcTemplate db;
 public AdminEvaluationController(NamedParameterJdbcTemplate db){this.db=db;}

 public record ReviewRequest(@NotBlank @Size(max=1000) String reason) {}

 @GetMapping("/campaigns/{campaignId}/progress")
 public List<Map<String,Object>> progress(@PathVariable Long campaignId) {
  assertCampaign(campaignId);
  return db.queryForList("""
   SELECT e.class_group_id, cg.name AS class_name, e.teacher_id, CONCAT(tp.first_name, ' ', tp.last_name) AS teacher_name, e.subject_id, su.name AS subject_name,
    COUNT(*) AS total_started,
    COUNT(*) FILTER (WHERE e.status='DRAFT') AS drafts,
    COUNT(*) FILTER (WHERE e.status='RETURNED') AS returned,
    COUNT(*) FILTER (WHERE e.status='SUBMITTED') AS submitted,
    COUNT(*) FILTER (WHERE e.status='APPROVED') AS approved
   FROM term_student_evaluation e
   LEFT JOIN class_group cg ON cg.id=e.class_group_id
   LEFT JOIN teacher t ON t.id=e.teacher_id
   LEFT JOIN employee emp ON emp.id=t.employee_id
   LEFT JOIN person tp ON tp.id=emp.person_id
   LEFT JOIN subject su ON su.id=e.subject_id
   WHERE e.campaign_id=:campaign
   GROUP BY e.class_group_id,cg.name,e.teacher_id,tp.first_name,tp.last_name,e.subject_id,su.name
   ORDER BY e.class_group_id,e.teacher_id,e.subject_id
   """, Map.of("campaign",campaignId));
 }

 @GetMapping("/campaigns/{campaignId}/evaluations")
 public List<Map<String,Object>> evaluations(@PathVariable Long campaignId,
    @RequestParam(required=false) String status,
    @RequestParam(required=false) Long classGroupId,
    @RequestParam(required=false) Long subjectId,
    @RequestParam(required=false) Long levelId) {
  assertCampaign(campaignId);
  if(status!=null && !Set.of("DRAFT","SUBMITTED","RETURNED","APPROVED").contains(status))
   throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Statut inconnu");
  var p=new MapSqlParameterSource().addValue("campaign",campaignId)
   .addValue("status",status).addValue("classId",classGroupId);
  StringBuilder sql=new StringBuilder("""
    SELECT e.*, CONCAT(sp.first_name, ' ', sp.last_name) AS student_name,
    CONCAT(tp.first_name, ' ', tp.last_name) AS teacher_name,
    su.name AS subject_name, cg.name AS class_name, cg.level_id, lvl.name AS level_name
    FROM term_student_evaluation e
    LEFT JOIN student st ON st.id=e.student_id
    LEFT JOIN person sp ON sp.id=st.person_id
    LEFT JOIN teacher t ON t.id=e.teacher_id
    LEFT JOIN employee emp ON emp.id=t.employee_id
    LEFT JOIN person tp ON tp.id=emp.person_id
    LEFT JOIN subject su ON su.id=e.subject_id
    LEFT JOIN class_group cg ON cg.id=e.class_group_id
    LEFT JOIN level lvl ON lvl.id=cg.level_id
    WHERE e.campaign_id=:campaign
    """);
  if(status!=null)sql.append(" AND e.status=:status");
  if(classGroupId!=null)sql.append(" AND e.class_group_id=:classId");
  if(subjectId!=null){sql.append(" AND e.subject_id=:subjectId");p.addValue("subjectId",subjectId);}
  if(levelId!=null){sql.append(" AND cg.level_id=:levelId");p.addValue("levelId",levelId);}
  sql.append(" ORDER BY e.class_group_id,e.student_id,e.subject_id");
  return db.queryForList(sql.toString(),p);
 }

 @GetMapping("/evaluations/{id}")
 public Map<String,Object> detail(@PathVariable Long id) {
  var rows=db.queryForList("SELECT * FROM term_student_evaluation WHERE id=:id",Map.of("id",id));
  if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Évaluation introuvable");
  var result=new LinkedHashMap<String,Object>(rows.get(0));
  result.putAll(db.queryForMap("""
   SELECT CONCAT(sp.first_name, ' ', sp.last_name) AS student_name,
    CONCAT(tp.first_name, ' ', tp.last_name) AS teacher_name,
    su.name AS subject_name, cg.name AS class_name, cg.level_id, lvl.name AS level_name
   FROM term_student_evaluation e
   LEFT JOIN student st ON st.id=e.student_id LEFT JOIN person sp ON sp.id=st.person_id
   LEFT JOIN teacher t ON t.id=e.teacher_id LEFT JOIN employee emp ON emp.id=t.employee_id
   LEFT JOIN person tp ON tp.id=emp.person_id LEFT JOIN subject su ON su.id=e.subject_id
   LEFT JOIN class_group cg ON cg.id=e.class_group_id LEFT JOIN level lvl ON lvl.id=cg.level_id WHERE e.id=:id
   """,Map.of("id",id))); 
  result.put("reviews",db.queryForList("SELECT action,reason,created_at FROM term_evaluation_review_log WHERE evaluation_id=:id ORDER BY created_at DESC",Map.of("id",id)));
  result.put("answers",db.queryForList("""
   SELECT a.criterion_id,a.option_id,a.free_text,c.code AS criterion_code,
    c.label_fr AS criterion_label_fr,c.label_ar AS criterion_label_ar,
    o.label_fr AS option_label_fr,o.label_ar AS option_label_ar
   FROM term_evaluation_answer a
   JOIN term_evaluation_criterion c ON c.id=a.criterion_id
   LEFT JOIN term_evaluation_option o ON o.id=a.option_id
   WHERE a.evaluation_id=:id ORDER BY c.section,c.sort_order
   """,Map.of("id",id)));
  return result;
 }

 @PostMapping("/evaluations/{id}/approve")
 @Transactional
 public Map<String,Object> approve(@PathVariable Long id){
  var rows=db.queryForList("""
   UPDATE term_student_evaluation SET status='APPROVED',updated_at=now()
   WHERE id=:id AND status='SUBMITTED'
   RETURNING id,status,updated_at
   """,Map.of("id",id));
  if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.CONFLICT,"Évaluation non soumise");
  return rows.get(0);
 }

 @PostMapping("/evaluations/{id}/return")
 @Transactional
 public Map<String,Object> returnForCorrection(@PathVariable Long id,@Valid @RequestBody ReviewRequest body){
  var rows=db.queryForList("""
   UPDATE term_student_evaluation SET status='RETURNED',updated_at=now()
   WHERE id=:id AND status IN ('SUBMITTED','APPROVED')
   RETURNING id,status,updated_at
   """,Map.of("id",id));
  if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.CONFLICT,"Retour impossible");
  // Raison conservée dans l'historique dédié au module.
  db.update("""
   INSERT INTO term_evaluation_review_log(evaluation_id,action,reason,created_at)
   VALUES(:id,'RETURNED',:reason,now())
   """,new MapSqlParameterSource().addValue("id",id).addValue("reason",body.reason()));
  return rows.get(0);
 }

 public record BulkApprovalRequest(List<Long> ids) {}
 @PostMapping("/evaluations/approve-bulk")
 @Transactional
 public Map<String,Object> approveBulk(@RequestBody BulkApprovalRequest request){
  if(request==null || request.ids()==null || request.ids().isEmpty() || request.ids().size()>500)
   throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Sélection invalide (1 à 500 évaluations)");
  var ids=new LinkedHashSet<>(request.ids());
  if(ids.contains(null))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Identifiant invalide");
  var p=new MapSqlParameterSource("ids",ids);
  var eligible=db.queryForObject("SELECT count(*) FROM term_student_evaluation WHERE id IN (:ids) AND status='SUBMITTED'",p,Long.class);
  if(eligible==null || eligible!=ids.size())throw new ResponseStatusException(HttpStatus.CONFLICT,"La sélection contient une évaluation non soumise : aucune approbation effectuée");
  int updated=db.update("UPDATE term_student_evaluation SET status='APPROVED',updated_at=now() WHERE id IN (:ids) AND status='SUBMITTED'",p);
  return Map.of("approved",updated);
 }
 @GetMapping("/campaigns/{campaignId}/expected")
 public List<Map<String,Object>> expected(@PathVariable Long campaignId){
  assertCampaign(campaignId);
  return db.queryForList("""
   WITH assignments AS (
    SELECT DISTINCT a.teacher_id,a.subject_id,a.class_group_id,c.school_year_id
    FROM teaching_assignment a JOIN term_evaluation_campaign c ON c.school_year_id=a.school_year_id
    JOIN class_group cg ON cg.id=a.class_group_id
    JOIN level l ON l.id=cg.level_id JOIN cycle cy ON cy.id=l.cycle_id
    WHERE c.id=:campaign AND a.status='ACTIVE' AND a.start_date<=c.evaluation_to
      AND (a.end_date IS NULL OR a.end_date>=c.evaluation_from)
      AND (c.cycle_code IS NULL OR c.cycle_code=cy.code)
      AND (NOT EXISTS(SELECT 1 FROM term_evaluation_campaign_class cc WHERE cc.campaign_id=c.id)
        OR EXISTS(SELECT 1 FROM term_evaluation_campaign_class cc WHERE cc.campaign_id=c.id AND cc.class_group_id=a.class_group_id))
   ), expected AS (
    SELECT DISTINCT en.student_id,a.teacher_id,a.subject_id,a.class_group_id
    FROM assignments a JOIN student_enrollment en ON en.school_year_id=a.school_year_id
     AND en.current_class_group_id=a.class_group_id AND en.status='ACTIVE'
   )
   SELECT x.class_group_id,cg.name AS class_name,x.teacher_id,
    CONCAT(p.first_name,' ',p.last_name) AS teacher_name,x.subject_id,s.name AS subject_name,
    count(*) AS expected_count,
    count(e.id) FILTER (WHERE e.status='DRAFT') AS draft_count,
    count(e.id) FILTER (WHERE e.status='SUBMITTED') AS submitted_count,
    count(e.id) FILTER (WHERE e.status='RETURNED') AS returned_count,
    count(e.id) FILTER (WHERE e.status='APPROVED') AS approved_count,
    count(*)-count(e.id) AS missing_count
   FROM expected x
   JOIN class_group cg ON cg.id=x.class_group_id
   JOIN subject s ON s.id=x.subject_id
   JOIN teacher t ON t.id=x.teacher_id JOIN employee emp ON emp.id=t.employee_id
   JOIN person p ON p.id=emp.person_id
   LEFT JOIN term_student_evaluation e ON e.campaign_id=:campaign AND e.student_id=x.student_id
     AND e.teacher_id=x.teacher_id AND e.subject_id=x.subject_id AND e.class_group_id=x.class_group_id
   GROUP BY x.class_group_id,cg.name,x.teacher_id,p.first_name,p.last_name,x.subject_id,s.name
   ORDER BY cg.name,p.last_name,s.name
   """,Map.of("campaign",campaignId));
 }
 private void assertCampaign(Long id){
  var rows=db.queryForList("SELECT id FROM term_evaluation_campaign WHERE id=:id",Map.of("id",id));
  if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Campagne introuvable");
 }
}
