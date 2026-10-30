package com.ecole.gestion_scolaire.termevaluation;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

/** Read-only statistics based on expected active assignments, including evaluations never created. */
@RestController
@RequestMapping("/api/term-evaluations/admin/campaigns/{campaignId}/statistics")
@PreAuthorize("hasAuthority('EVALUATION_TRIMESTRIELLE_CONTROLER')")
public class AdminEvaluationStatisticsController {
 private final NamedParameterJdbcTemplate db;
 public AdminEvaluationStatisticsController(NamedParameterJdbcTemplate db) { this.db = db; }

 private static final Set<String> STATUSES = Set.of("NOT_STARTED", "DRAFT", "RETURNED", "SUBMITTED", "APPROVED", "COMPLETED", "PENDING");

 // The campaign's expected work is one evaluation per active student / teacher / subject / class.
 // DISTINCT prevents duplicate teaching assignments from inflating totals.
 private static final String EXPECTED = """
  WITH expected AS (
   SELECT DISTINCT c.id AS campaign_id, a.teacher_id, a.subject_id, a.class_group_id,
     en.student_id, cg.level_id, cg.name AS class_name, su.name AS subject_name,
     CONCAT_WS(' ', tp.first_name, tp.last_name) AS teacher_name,
     CONCAT_WS(' ', sp.first_name, sp.last_name) AS student_name
   FROM term_evaluation_campaign c
   JOIN teaching_assignment a ON a.school_year_id=c.school_year_id
    AND a.status='ACTIVE' AND a.start_date<=c.evaluation_to
    AND (a.end_date IS NULL OR a.end_date>=c.evaluation_from)
   JOIN class_group cg ON cg.id=a.class_group_id AND cg.school_year_id=c.school_year_id
   JOIN level lvl ON lvl.id=cg.level_id
   JOIN cycle cyc ON cyc.id=lvl.cycle_id
   JOIN student_enrollment en ON en.school_year_id=c.school_year_id
    AND en.current_class_group_id=a.class_group_id AND en.status='ACTIVE'
   JOIN student st ON st.id=en.student_id
   JOIN person sp ON sp.id=st.person_id
   JOIN subject su ON su.id=a.subject_id
   JOIN teacher t ON t.id=a.teacher_id
   JOIN employee emp ON emp.id=t.employee_id
   JOIN person tp ON tp.id=emp.person_id
   WHERE c.id=:campaignId
    AND (c.cycle_code IS NULL OR c.cycle_code=cyc.code)
    AND (NOT EXISTS (SELECT 1 FROM term_evaluation_campaign_class cc WHERE cc.campaign_id=c.id)
     OR EXISTS (SELECT 1 FROM term_evaluation_campaign_class cc
      WHERE cc.campaign_id=c.id AND cc.class_group_id=a.class_group_id))
  ), workload AS (
   SELECT x.*, e.id AS evaluation_id, COALESCE(e.status,'NOT_STARTED') AS evaluation_status,
    e.updated_at AS started_at, e.updated_at AS updated_at
   FROM expected x LEFT JOIN term_student_evaluation e
    ON e.campaign_id=x.campaign_id AND e.student_id=x.student_id
    AND e.teacher_id=x.teacher_id AND e.subject_id=x.subject_id
    AND e.class_group_id=x.class_group_id
  )
  """;

 private void assertCampaign(Long id) {
  Boolean exists=db.queryForObject("SELECT EXISTS(SELECT 1 FROM term_evaluation_campaign WHERE id=:id)",Map.of("id",id),Boolean.class);
  if (!Boolean.TRUE.equals(exists)) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Campagne introuvable");
 }
 private MapSqlParameterSource params(Long campaignId, Long teacherId, Long classGroupId, Long subjectId, Long levelId, String status) {
  if(status!=null && !STATUSES.contains(status)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Statut de filtre invalide");
  return new MapSqlParameterSource().addValue("campaignId",campaignId).addValue("teacherId",teacherId)
   .addValue("classGroupId",classGroupId).addValue("subjectId",subjectId).addValue("levelId",levelId).addValue("status",status);
 }
 private static final String FILTERS="""
   WHERE (CAST(:teacherId AS BIGINT) IS NULL OR teacher_id=:teacherId)
    AND (CAST(:classGroupId AS BIGINT) IS NULL OR class_group_id=:classGroupId)
    AND (CAST(:subjectId AS BIGINT) IS NULL OR subject_id=:subjectId)
    AND (CAST(:levelId AS BIGINT) IS NULL OR level_id=:levelId)
    AND (CAST(:status AS VARCHAR) IS NULL OR evaluation_status=:status
      OR (:status='COMPLETED' AND evaluation_status IN ('SUBMITTED','APPROVED'))
      OR (:status='PENDING' AND evaluation_status IN ('NOT_STARTED','DRAFT','RETURNED')))
  """;
 private static final String COUNTS="""
   COUNT(*) AS expected_count,
   COUNT(*) FILTER (WHERE evaluation_status='NOT_STARTED') AS not_started_count,
   COUNT(*) FILTER (WHERE evaluation_status='DRAFT') AS draft_count,
   COUNT(*) FILTER (WHERE evaluation_status='RETURNED') AS returned_count,
   COUNT(*) FILTER (WHERE evaluation_status='SUBMITTED') AS submitted_count,
   COUNT(*) FILTER (WHERE evaluation_status='APPROVED') AS approved_count,
   COUNT(*) FILTER (WHERE evaluation_status IN ('SUBMITTED','APPROVED')) AS completed_count,
   COUNT(*) FILTER (WHERE evaluation_status IN ('NOT_STARTED','DRAFT','RETURNED')) AS pending_count,
   COUNT(DISTINCT student_id) AS student_count,
   ROUND(100.0 * COUNT(*) FILTER (WHERE evaluation_status IN ('SUBMITTED','APPROVED')) / NULLIF(COUNT(*),0), 1) AS completion_percent
  """;

 /** Global campaign KPIs (optionally scoped to teacher, class, subject, level or status). */
 @GetMapping("/summary")
 public Map<String,Object> summary(@PathVariable Long campaignId,
   @RequestParam(required=false) Long teacherId,@RequestParam(required=false) Long classGroupId,
   @RequestParam(required=false) Long subjectId,@RequestParam(required=false) Long levelId,
   @RequestParam(required=false) String status) {
  assertCampaign(campaignId);
  var p=params(campaignId,teacherId,classGroupId,subjectId,levelId,status);
  return db.queryForMap(EXPECTED+"SELECT "+COUNTS+" ,COUNT(DISTINCT teacher_id) AS teacher_count, " +
    "COUNT(DISTINCT teacher_id) FILTER (WHERE evaluation_status IN ('SUBMITTED','APPROVED')) AS teachers_with_submissions, " +
    "COUNT(DISTINCT teacher_id) FILTER (WHERE evaluation_status IN ('NOT_STARTED','DRAFT','RETURNED')) AS teachers_with_pending " +
    "FROM workload "+FILTERS,p);
 }

 /** One row per teacher, with progress across all their classes and subjects. */
 @GetMapping("/teachers")
 public List<Map<String,Object>> teachers(@PathVariable Long campaignId,
   @RequestParam(required=false) Long teacherId,@RequestParam(required=false) Long classGroupId,
   @RequestParam(required=false) Long subjectId,@RequestParam(required=false) Long levelId,
   @RequestParam(required=false) String status) {
  assertCampaign(campaignId);
  return db.queryForList(EXPECTED+"SELECT teacher_id,teacher_name, COUNT(DISTINCT class_group_id) AS class_count, " +
   "COUNT(DISTINCT subject_id) AS subject_count, "+COUNTS+" FROM workload "+FILTERS+
   " GROUP BY teacher_id,teacher_name ORDER BY pending_count DESC,teacher_name",params(campaignId,teacherId,classGroupId,subjectId,levelId,status));
 }

 /** Teacher / class / subject breakdown; supports all dashboard filters. */
 @GetMapping("/assignments")
 public List<Map<String,Object>> assignments(@PathVariable Long campaignId,
   @RequestParam(required=false) Long teacherId,@RequestParam(required=false) Long classGroupId,
   @RequestParam(required=false) Long subjectId,@RequestParam(required=false) Long levelId,
   @RequestParam(required=false) String status) {
  assertCampaign(campaignId);
  return db.queryForList(EXPECTED+"SELECT teacher_id,teacher_name,class_group_id,class_name,level_id,subject_id,subject_name, "+COUNTS+
   " FROM workload "+FILTERS+" GROUP BY teacher_id,teacher_name,class_group_id,class_name,level_id,subject_id,subject_name " +
   "ORDER BY teacher_name,class_name,subject_name",params(campaignId,teacherId,classGroupId,subjectId,levelId,status));
 }

 /** Student-level drilldown, including students for whom the teacher has not started an evaluation. */
 @GetMapping("/students")
 public List<Map<String,Object>> students(@PathVariable Long campaignId,
   @RequestParam(required=false) Long teacherId,@RequestParam(required=false) Long classGroupId,
   @RequestParam(required=false) Long subjectId,@RequestParam(required=false) Long levelId,
   @RequestParam(required=false) String status) {
  assertCampaign(campaignId);
  return db.queryForList(EXPECTED+"SELECT teacher_id,teacher_name,class_group_id,class_name,level_id,subject_id,subject_name, " +
   "student_id,student_name,evaluation_id,evaluation_status,started_at,updated_at FROM workload "+FILTERS+
   " ORDER BY teacher_name,class_name,subject_name,student_name",params(campaignId,teacherId,classGroupId,subjectId,levelId,status));
 }
}
