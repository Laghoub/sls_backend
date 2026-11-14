package com.ecole.gestion_scolaire.termevaluation;
import org.springframework.http.*;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
@RestController
@RequestMapping("/api/term-evaluations/publications")
public class PublicationController {
 private final NamedParameterJdbcTemplate db;
 public PublicationController(NamedParameterJdbcTemplate db){this.db=db;}
 private static final String EXPECTED = """
  WITH expected AS (
   SELECT DISTINCT en.student_id,a.teacher_id,a.subject_id,a.class_group_id
   FROM term_evaluation_campaign c
   JOIN class_group cg ON cg.school_year_id=c.school_year_id
   JOIN level l ON l.id=cg.level_id JOIN cycle cy ON cy.id=l.cycle_id
   JOIN student_enrollment en ON en.school_year_id=c.school_year_id
    AND en.current_class_group_id=cg.id AND en.status='ACTIVE'
   JOIN teaching_assignment a ON a.school_year_id=c.school_year_id
    AND a.class_group_id=cg.id AND a.status='ACTIVE'
    AND a.start_date<=c.evaluation_to AND (a.end_date IS NULL OR a.end_date>=c.evaluation_from)
   WHERE c.id=:campaign AND cg.id=:classId
    AND (c.cycle_code IS NULL OR c.cycle_code=cy.code)
    AND (NOT EXISTS(SELECT 1 FROM term_evaluation_campaign_class cc WHERE cc.campaign_id=c.id)
     OR EXISTS(SELECT 1 FROM term_evaluation_campaign_class cc WHERE cc.campaign_id=c.id AND cc.class_group_id=cg.id))
  )
  """;

 /** État réel de publication par classe, sans dépendre des filtres du tableau. */
 @GetMapping("/campaigns/{campaignId}/classes")
 @PreAuthorize("hasAuthority('EVALUATION_TRIMESTRIELLE_CONTROLER')")
 public List<Map<String,Object>> classes(@PathVariable Long campaignId){
  if(Boolean.FALSE.equals(db.queryForObject("SELECT EXISTS(SELECT 1 FROM term_evaluation_campaign WHERE id=:id)",Map.of("id",campaignId),Boolean.class)))
   throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Campagne introuvable");
  var classRows=db.queryForList("""
   SELECT cg.id AS class_group_id,cg.name AS class_name,
    (SELECT count(DISTINCT en.student_id) FROM student_enrollment en
     WHERE en.school_year_id=c.school_year_id AND en.current_class_group_id=cg.id AND en.status='ACTIVE') AS student_count,
    (SELECT count(*) FROM teaching_assignment a WHERE a.school_year_id=c.school_year_id AND a.class_group_id=cg.id
     AND a.status='ACTIVE' AND a.start_date<=c.evaluation_to AND (a.end_date IS NULL OR a.end_date>=c.evaluation_from)) AS assignment_count,
    (SELECT count(*) FROM term_evaluation_publication pub JOIN student_enrollment en ON en.student_id=pub.student_id
     WHERE pub.campaign_id=c.id AND en.school_year_id=c.school_year_id AND en.current_class_group_id=cg.id
      AND en.status='ACTIVE') AS publication_count
   FROM term_evaluation_campaign c JOIN class_group cg ON cg.school_year_id=c.school_year_id
   JOIN level l ON l.id=cg.level_id JOIN cycle cy ON cy.id=l.cycle_id
   WHERE c.id=:campaign AND (c.cycle_code IS NULL OR c.cycle_code=cy.code)
    AND (NOT EXISTS(SELECT 1 FROM term_evaluation_campaign_class cc WHERE cc.campaign_id=c.id)
     OR EXISTS(SELECT 1 FROM term_evaluation_campaign_class cc WHERE cc.campaign_id=c.id AND cc.class_group_id=cg.id))
   ORDER BY cg.name
   """,Map.of("campaign",campaignId));
  for(var row:classRows){
   var params=new MapSqlParameterSource().addValue("campaign",campaignId)
    .addValue("classId",((Number)row.get("class_group_id")).longValue());
   var counts=db.queryForMap(EXPECTED+"""
    SELECT count(*) AS expected_count,count(*) FILTER (WHERE e.status='APPROVED') AS approved_count
    FROM expected x LEFT JOIN term_student_evaluation e ON e.campaign_id=:campaign
     AND e.student_id=x.student_id AND e.teacher_id=x.teacher_id
     AND e.subject_id=x.subject_id AND e.class_group_id=x.class_group_id
    """,params);
   long expected=((Number)counts.get("expected_count")).longValue();
   long approved=((Number)counts.get("approved_count")).longValue();
   long studentCount=((Number)row.get("student_count")).longValue();
   long publicationCount=((Number)row.get("publication_count")).longValue();
   boolean isPublished=publicationCount>0;
   row.put("expected_count",expected);
   row.put("approved_count",approved);
   row.put("approval_percent",expected==0?0:Math.round(approved*1000.0/expected)/10.0);
   row.put("publication_status",isPublished?(publicationCount==studentCount?"PUBLISHED":"PARTIALLY_PUBLISHED"):"NOT_PUBLISHED");
   row.put("can_publish",studentCount>0 && expected>0 && approved==expected && !isPublished);
  }
  return classRows;
 }

 /** Publication tout-ou-rien, seulement si chaque évaluation attendue est APPROVED. */
 @PostMapping("/campaigns/{campaignId}/classes/{classId}")
 @PreAuthorize("hasAuthority('EVALUATION_TRIMESTRIELLE_PUBLIER')")
 @Transactional
 public Map<String,Object> publishClass(@PathVariable Long campaignId,@PathVariable Long classId){
  var p=new MapSqlParameterSource().addValue("campaign",campaignId).addValue("classId",classId);
  // Sérialise les publications concurrentes d'une même campagne.
  var campaign=db.queryForList("SELECT id FROM term_evaluation_campaign WHERE id=:campaign FOR UPDATE",p);
  if(campaign.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Campagne introuvable");
  var students=db.queryForList("""
   SELECT DISTINCT en.student_id FROM student_enrollment en
   JOIN term_evaluation_campaign c ON c.school_year_id=en.school_year_id
   JOIN class_group cg ON cg.id=en.current_class_group_id
   JOIN level l ON l.id=cg.level_id JOIN cycle cy ON cy.id=l.cycle_id
   WHERE c.id=:campaign AND cg.id=:classId AND en.status='ACTIVE'
    AND (c.cycle_code IS NULL OR c.cycle_code=cy.code)
    AND (NOT EXISTS(SELECT 1 FROM term_evaluation_campaign_class cc WHERE cc.campaign_id=c.id)
     OR EXISTS(SELECT 1 FROM term_evaluation_campaign_class cc WHERE cc.campaign_id=c.id AND cc.class_group_id=cg.id))
   ORDER BY en.student_id
   """,p);
  if(students.isEmpty())throw new ResponseStatusException(HttpStatus.CONFLICT,"Classe sans élèves actifs ou hors campagne");
  var totals=db.queryForMap(EXPECTED+"""
   SELECT count(*) AS expected_count,
    count(*) FILTER (WHERE e.status='APPROVED') AS approved_count
   FROM expected x LEFT JOIN term_student_evaluation e
    ON e.campaign_id=:campaign AND e.student_id=x.student_id AND e.teacher_id=x.teacher_id
    AND e.subject_id=x.subject_id AND e.class_group_id=x.class_group_id
   """,p);
  long expected=((Number)totals.get("expected_count")).longValue();
  long approved=((Number)totals.get("approved_count")).longValue();
  if(expected==0 || expected!=approved)throw new ResponseStatusException(HttpStatus.CONFLICT,
   "Publication refusée : toutes les évaluations de la classe doivent être approuvées ("+approved+"/"+expected+")");
  var existing=db.queryForObject("""
   SELECT count(DISTINCT pub.student_id) FROM term_evaluation_publication pub
   JOIN student_enrollment en ON en.student_id=pub.student_id
   JOIN term_evaluation_campaign c ON c.school_year_id=en.school_year_id
   WHERE c.id=:campaign AND pub.campaign_id=:campaign AND en.current_class_group_id=:classId AND en.status='ACTIVE'
   """,p,Long.class);
  if(existing!=null && existing>0)throw new ResponseStatusException(HttpStatus.CONFLICT,
   "Publication déjà effectuée pour au moins un élève de cette classe");
  // Vérifie l'absence d'évaluations hors périmètre non approuvées avant toute insertion.
  var incomplete=db.queryForObject("""
   SELECT count(*) FROM term_student_evaluation e
   WHERE e.campaign_id=:campaign AND e.class_group_id=:classId AND e.status<>'APPROVED'
   """,p,Long.class);
  if(incomplete!=null && incomplete>0)throw new ResponseStatusException(HttpStatus.CONFLICT,"Des évaluations de la classe ne sont pas approuvées");
  int published=0;
  for(var student:students){
   publishStudent(campaignId,((Number)student.get("student_id")).longValue());
   published++;
  }
  return Map.of("campaignId",campaignId,"classGroupId",classId,"status","PUBLISHED","publishedCount",published);
 }

 // Publication individuelle désactivée : seule la publication atomique par classe est exposée.
 private Map<String,Object> publishStudent(Long campaignId,Long studentId){
  var params=new MapSqlParameterSource().addValue("campaign",campaignId).addValue("student",studentId);
  var rows=db.queryForList("""
   SELECT e.id,e.subject_id,e.teacher_id,e.class_group_id,e.observation,e.difficulties,e.recommendations,
    COALESCE((SELECT jsonb_agg(jsonb_build_object('criterion',c.code,'section',c.section,
      'option',o.code,'optionAr',o.label_ar,'optionFr',o.label_fr,'text',a.free_text))
     FROM term_evaluation_answer a JOIN term_evaluation_criterion c ON c.id=a.criterion_id
     LEFT JOIN term_evaluation_option o ON o.id=a.option_id WHERE a.evaluation_id=e.id),'[]'::jsonb)::text AS answers_json
   FROM term_student_evaluation e
   WHERE e.campaign_id=:campaign AND e.student_id=:student AND e.status='APPROVED'
   ORDER BY e.subject_id,e.teacher_id
   """,params);
  if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.CONFLICT,"Aucune évaluation approuvée");
  // Refuser une publication partielle : toutes les évaluations déjà saisies doivent être approuvées.
  var incomplete=db.queryForObject("""
   SELECT count(*) FROM term_student_evaluation
   WHERE campaign_id=:campaign AND student_id=:student AND status<>'APPROVED'
   """,params,Long.class);
  if(incomplete!=null && incomplete>0)throw new ResponseStatusException(HttpStatus.CONFLICT,"Évaluations non approuvées");
  // Contrôle de complétude : une évaluation par élève / matière / enseignant attendu.
  // Une évaluation jamais créée doit également bloquer la publication.
  var missing=db.queryForObject("""
   WITH expected AS (
    SELECT DISTINCT a.teacher_id,a.subject_id,a.class_group_id
    FROM term_evaluation_campaign c
    JOIN student_enrollment en ON en.student_id=:student AND en.school_year_id=c.school_year_id AND en.status='ACTIVE'
    JOIN teaching_assignment a ON a.school_year_id=c.school_year_id AND a.class_group_id=en.current_class_group_id
      AND a.status='ACTIVE' AND a.start_date<=c.evaluation_to AND (a.end_date IS NULL OR a.end_date>=c.evaluation_from)
    JOIN class_group cg ON cg.id=a.class_group_id JOIN level l ON l.id=cg.level_id JOIN cycle cy ON cy.id=l.cycle_id
    WHERE c.id=:campaign AND (c.cycle_code IS NULL OR c.cycle_code=cy.code)
      AND (NOT EXISTS(SELECT 1 FROM term_evaluation_campaign_class cc WHERE cc.campaign_id=c.id)
        OR EXISTS(SELECT 1 FROM term_evaluation_campaign_class cc WHERE cc.campaign_id=c.id AND cc.class_group_id=a.class_group_id))
   )
   SELECT count(*) FROM expected x WHERE NOT EXISTS (
    SELECT 1 FROM term_student_evaluation e WHERE e.campaign_id=:campaign AND e.student_id=:student
     AND e.teacher_id=x.teacher_id AND e.subject_id=x.subject_id AND e.class_group_id=x.class_group_id AND e.status='APPROVED')
   """,params,Long.class);
  if(missing==null || missing>0)throw new ResponseStatusException(HttpStatus.CONFLICT,
   "Publication impossible : "+missing+" évaluation(s) attendue(s) manquante(s) ou non approuvée(s)");
  var totalExpected=db.queryForObject("""
   SELECT count(DISTINCT (a.teacher_id,a.subject_id,a.class_group_id))
   FROM term_evaluation_campaign c JOIN student_enrollment en ON en.student_id=:student
    AND en.school_year_id=c.school_year_id AND en.status='ACTIVE'
   JOIN teaching_assignment a ON a.school_year_id=c.school_year_id AND a.class_group_id=en.current_class_group_id
    AND a.status='ACTIVE' AND a.start_date<=c.evaluation_to AND (a.end_date IS NULL OR a.end_date>=c.evaluation_from)
   JOIN class_group cg ON cg.id=a.class_group_id JOIN level l ON l.id=cg.level_id JOIN cycle cy ON cy.id=l.cycle_id
   WHERE c.id=:campaign AND (c.cycle_code IS NULL OR c.cycle_code=cy.code)
    AND (NOT EXISTS(SELECT 1 FROM term_evaluation_campaign_class cc WHERE cc.campaign_id=c.id)
     OR EXISTS(SELECT 1 FROM term_evaluation_campaign_class cc WHERE cc.campaign_id=c.id AND cc.class_group_id=a.class_group_id))
   """,params,Long.class);
  if(totalExpected==null || totalExpected==0)throw new ResponseStatusException(HttpStatus.CONFLICT,"Aucune affectation attendue pour cet élève");
  // Une publication ne doit jamais contenir des noms de matières ou d'élève manquants.
  var invalid=db.queryForObject("""
   SELECT count(*) FROM term_student_evaluation e
   LEFT JOIN subject su ON su.id=e.subject_id
   WHERE e.campaign_id=:campaign AND e.student_id=:student AND e.status='APPROVED'
     AND (su.name IS NULL OR btrim(su.name)='')
   """,params,Long.class);
  if(invalid!=null && invalid>0)throw new ResponseStatusException(HttpStatus.CONFLICT,"Matière non renseignée : publication refusée");
  // Contrôler les métadonnées AVANT d'insérer une publication immuable.
  var identity=db.queryForMap("""
   SELECT nullif(btrim(concat_ws(' ',p.first_name,p.last_name)),'') AS student_name,
     (SELECT nullif(btrim(cg.name),'') FROM student_enrollment en
      JOIN class_group cg ON cg.id=en.current_class_group_id
      JOIN term_evaluation_campaign c ON c.school_year_id=en.school_year_id
      WHERE c.id=:campaign AND en.student_id=:student AND en.status='ACTIVE'
      ORDER BY en.id DESC LIMIT 1) AS class_name
   FROM student st JOIN person p ON p.id=st.person_id WHERE st.id=:student
   """,params);
  if(identity.get("student_name")==null || identity.get("class_name")==null)
   throw new ResponseStatusException(HttpStatus.CONFLICT,
    "Publication refusée : nom de l'élève ou classe introuvable dans l'année de la campagne");
  // JSON construit par PostgreSQL, sans concaténation de données non fiables.
  var published=db.queryForMap("""
   WITH payload AS (
    SELECT jsonb_build_object('campaignId',:campaign,'studentId',:student,
      'studentName',(SELECT concat(p.first_name,' ',p.last_name) FROM student st JOIN person p ON p.id=st.person_id WHERE st.id=:student),
      'className',(SELECT cg.name FROM student_enrollment en JOIN class_group cg ON cg.id=en.current_class_group_id
        JOIN term_evaluation_campaign c ON c.school_year_id=en.school_year_id WHERE c.id=:campaign AND en.student_id=:student AND en.status='ACTIVE' ORDER BY en.id DESC LIMIT 1),
      'evaluations',COALESCE((SELECT jsonb_agg(jsonb_build_object(
       'subjectId',e.subject_id,'subjectName',su.name,'teacherId',e.teacher_id,
       'teacherName',concat(tp.first_name,' ',tp.last_name),
       'observation',e.observation,'difficulties',e.difficulties,'recommendations',e.recommendations,
       'answers',(SELECT coalesce(jsonb_agg(jsonb_build_object('criterion',cr.code,'section',cr.section,
         'label',cr.label_fr,'labelFr',cr.label_fr,'option',op.label_fr,'optionFr',op.label_fr,'optionAr',op.label_ar,'freeText',ans.free_text) ORDER BY cr.section,cr.sort_order),'[]'::jsonb)
         FROM term_evaluation_answer ans JOIN term_evaluation_criterion cr ON cr.id=ans.criterion_id
         LEFT JOIN term_evaluation_option op ON op.id=ans.option_id WHERE ans.evaluation_id=e.id)) ORDER BY su.name)
       FROM term_student_evaluation e JOIN subject su ON su.id=e.subject_id
       LEFT JOIN teacher t ON t.id=e.teacher_id LEFT JOIN employee emp ON emp.id=t.employee_id
       LEFT JOIN person tp ON tp.id=emp.person_id
       WHERE e.campaign_id=:campaign AND e.student_id=:student AND e.status='APPROVED'),'[]'::jsonb),
      'grades',coalesce((SELECT jsonb_agg(g ORDER BY g."subjectName",g."assessmentDate") FROM (
       SELECT su.name AS "subjectName",a.title AS "title",aty.name AS "type",a.assessment_date AS "assessmentDate",
        gr.score AS "score",a.max_score AS "maxScore",'assessment' AS "source"
       FROM grade gr JOIN assessment a ON a.id=gr.assessment_id
       JOIN assessment_type aty ON aty.id=a.assessment_type_id
       JOIN teaching_assignment ta ON ta.id=a.teaching_assignment_id JOIN subject su ON su.id=ta.subject_id
       JOIN term_evaluation_campaign c ON c.school_year_id=ta.school_year_id
       WHERE c.id=:campaign AND gr.student_id=:student AND a.assessment_date BETWEEN c.evaluation_from AND c.evaluation_to
        AND gr.score IS NOT NULL AND a.status NOT IN ('CANCELLED','DELETED')
       UNION ALL
       SELECT su.name AS "subjectName",te.title AS "title",'Évaluation' AS "type",te.evaluation_date AS "assessmentDate",
        teg.score AS "score",te.max_score AS "maxScore",'teacher_evaluation' AS "source"
       FROM teacher_evaluation_grade teg JOIN teacher_evaluation te ON te.id=teg.evaluation_id
       JOIN teaching_assignment ta ON ta.id=te.teaching_assignment_id JOIN subject su ON su.id=ta.subject_id
       JOIN term_evaluation_campaign c ON c.school_year_id=ta.school_year_id
       WHERE c.id=:campaign AND teg.student_id=:student AND te.evaluation_date BETWEEN c.evaluation_from AND c.evaluation_to
        AND teg.score IS NOT NULL
      ) g),'[]'::jsonb)
    ) AS data
   )
   INSERT INTO term_evaluation_publication(campaign_id,student_id,version_number,snapshot)
   SELECT :campaign,:student,coalesce((SELECT max(version_number)+1 FROM term_evaluation_publication
    WHERE campaign_id=:campaign AND student_id=:student),1),payload.data FROM payload
   RETURNING id,campaign_id,student_id,version_number,published_at
   """,params);
  return published;
 }
}
