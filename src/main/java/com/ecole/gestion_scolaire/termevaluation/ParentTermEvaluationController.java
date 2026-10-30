package com.ecole.gestion_scolaire.termevaluation;
import org.springframework.http.*;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
@RestController
@RequestMapping("/api/term-evaluations/parent")
public class ParentTermEvaluationController {
 private final NamedParameterJdbcTemplate db;
 public ParentTermEvaluationController(NamedParameterJdbcTemplate db){this.db=db;}
 private String username(){return SecurityContextHolder.getContext().getAuthentication().getName();}
 @GetMapping("/students/{studentId}/reports")
 @PreAuthorize("hasAuthority('EVALUATION_TRIMESTRIELLE_CONSULTER')")
 public List<Map<String,Object>> reports(@PathVariable Long studentId){
  var p=new MapSqlParameterSource().addValue("student",studentId).addValue("username",username());
  return db.queryForList("""
   SELECT pub.id,pub.campaign_id,pub.version_number,pub.published_at,c.title,c.trimester
   FROM term_evaluation_publication pub
   JOIN term_evaluation_campaign c ON c.id=pub.campaign_id
   WHERE pub.student_id=:student
   AND EXISTS(SELECT 1 FROM student_guardian sg JOIN guardian g ON g.id=sg.guardian_id
    JOIN user_account u ON u.person_id=g.person_id
    WHERE sg.student_id=pub.student_id AND sg.active=TRUE AND u.username=:username
    AND (sg.valid_from IS NULL OR sg.valid_from<=CURRENT_DATE)
    AND (sg.valid_until IS NULL OR sg.valid_until>=CURRENT_DATE))
   AND nullif(btrim(pub.snapshot->>'studentName'),'') IS NOT NULL
   AND nullif(btrim(pub.snapshot->>'className'),'') IS NOT NULL
   ORDER BY pub.published_at DESC
   """,p);
 }
 /** Only metadata is exposed before publication; no teacher evaluations or grades. */
 @GetMapping("/students/{studentId}/availability")
 @PreAuthorize("hasAuthority('EVALUATION_TRIMESTRIELLE_CONSULTER')")
 public List<Map<String,Object>> availability(@PathVariable Long studentId){
  var p=new MapSqlParameterSource().addValue("student",studentId).addValue("username",username());
  // Do not leak campaigns or children not linked to the authenticated guardian.
  var rows=db.queryForList("""
   SELECT c.id AS campaign_id,c.title,c.trimester,c.closes_at AS estimated_publication_at,
    pub.id AS report_id,pub.published_at,
    CASE WHEN pub.id IS NULL THEN 'NOT_AVAILABLE' ELSE 'AVAILABLE' END AS status
   FROM term_evaluation_campaign c
   JOIN student_enrollment en ON en.school_year_id=c.school_year_id AND en.student_id=:student
    AND en.status='ACTIVE'
   JOIN class_group cg ON cg.id=en.current_class_group_id
   JOIN level l ON l.id=cg.level_id JOIN cycle cy ON cy.id=l.cycle_id
   LEFT JOIN LATERAL (
     SELECT p.id,p.published_at FROM term_evaluation_publication p
     WHERE p.student_id=:student AND p.campaign_id=c.id
      AND nullif(btrim(p.snapshot->>'studentName'),'') IS NOT NULL
      AND nullif(btrim(p.snapshot->>'className'),'') IS NOT NULL
      AND jsonb_typeof(p.snapshot->'evaluations')='array'
      AND jsonb_array_length(p.snapshot->'evaluations')>0
      AND NOT EXISTS (SELECT 1 FROM jsonb_array_elements(p.snapshot->'evaluations') ev
        WHERE nullif(btrim(ev->>'subjectName'),'') IS NULL)
     ORDER BY p.version_number DESC LIMIT 1
   ) pub ON TRUE
   WHERE (c.cycle_code IS NULL OR c.cycle_code=cy.code)
    AND (NOT EXISTS(SELECT 1 FROM term_evaluation_campaign_class cc WHERE cc.campaign_id=c.id)
      OR EXISTS(SELECT 1 FROM term_evaluation_campaign_class cc WHERE cc.campaign_id=c.id AND cc.class_group_id=cg.id))
    AND c.status NOT IN ('DRAFT','ARCHIVED')
    AND EXISTS(SELECT 1 FROM student_guardian sg JOIN guardian g ON g.id=sg.guardian_id
      JOIN user_account u ON u.person_id=g.person_id
      WHERE sg.student_id=:student AND sg.active=TRUE AND u.username=:username
      AND (sg.valid_from IS NULL OR sg.valid_from<=CURRENT_DATE)
      AND (sg.valid_until IS NULL OR sg.valid_until>=CURRENT_DATE))
   ORDER BY c.opens_at DESC
   """,p);
  return rows;
 }

 @GetMapping(value="/reports/{id}/pdf",produces=MediaType.APPLICATION_PDF_VALUE)
 @PreAuthorize("hasAuthority('EVALUATION_TRIMESTRIELLE_CONSULTER')")
 public ResponseEntity<byte[]> downloadPdf(@PathVariable Long id){
  var p=new MapSqlParameterSource().addValue("id",id).addValue("username",username());
  var rows=db.queryForList("""
   SELECT pub.snapshot::text AS snapshot_json,pub.student_id,pub.campaign_id,pub.version_number,pub.published_at
   FROM term_evaluation_publication pub
   WHERE pub.id=:id AND EXISTS (
    SELECT 1 FROM student_guardian sg JOIN guardian g ON g.id=sg.guardian_id
    JOIN user_account u ON u.person_id=g.person_id
    WHERE sg.student_id=pub.student_id AND sg.active=TRUE AND u.username=:username
    AND (sg.valid_from IS NULL OR sg.valid_from<=CURRENT_DATE)
    AND (sg.valid_until IS NULL OR sg.valid_until>=CURRENT_DATE))
   """,p);
  if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Rapport indisponible");
  var row=rows.get(0);
  try{
   byte[] pdf=TermReportPdf.render((String)row.get("snapshot_json"),String.valueOf(row.get("published_at")));
   String name="rapport-trimestriel-"+row.get("student_id")+"-"+row.get("campaign_id")+"-v"+row.get("version_number")+".pdf";
   return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\""+name+"\"")
    .cacheControl(CacheControl.noStore()).contentLength(pdf.length).body(pdf);
  }catch(java.io.IOException e){throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"Impossible de générer le PDF",e);}
 }

}
