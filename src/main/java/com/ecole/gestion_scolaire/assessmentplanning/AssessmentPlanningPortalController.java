package com.ecole.gestion_scolaire.assessmentplanning;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

/** Published assessments scoped to the authenticated teacher or parent. */
@RestController
@RequestMapping("/api/assessment-planning/portal")
public class AssessmentPlanningPortalController {
 private final JdbcTemplate db;
 public AssessmentPlanningPortalController(JdbcTemplate db){this.db=db;}
 private String username(){return SecurityContextHolder.getContext().getAuthentication().getName();}

 /** All published assessment entries for the teacher's assigned class AND subject. */
 @GetMapping("/teacher")
 @PreAuthorize("hasAnyAuthority('PORTAIL_ENSEIGNANT_ACCEDER','PLANNING_EVALUATION_CONSULTER','ROLE_ADMIN')")
 public List<Map<String,Object>> teacher(){return db.queryForList("""
  SELECT DISTINCT s.id AS schedule_id,s.title,s.assessment_type,s.term,cg.id AS class_group_id,cg.name AS class_name,
   e.id AS entry_id,e.assessment_date,e.start_time,e.end_time,su.id AS subject_id,su.name AS subject_name,e.room,e.instructions
  FROM assessment_schedule s
  JOIN assessment_schedule_entry e ON e.schedule_id=s.id
  JOIN class_group cg ON cg.id=s.class_group_id
  JOIN subject su ON su.id=e.subject_id
  JOIN teaching_assignment ta ON ta.class_group_id=s.class_group_id AND ta.subject_id=e.subject_id
    AND ta.school_year_id=s.school_year_id
  JOIN teacher t ON t.id=ta.teacher_id JOIN employee emp ON emp.id=t.employee_id
  JOIN user_account ua ON ua.person_id=emp.person_id
  WHERE ua.username=? AND s.status='PUBLISHED'
    AND ta.start_date<=e.assessment_date
    AND (ta.end_date IS NULL OR ta.end_date>=e.assessment_date)
  ORDER BY e.assessment_date,e.start_time,e.id
  """,username());}

 /** Published entries for all children belonging to the authenticated guardian. */
 @GetMapping("/parent")
 @PreAuthorize("hasAnyAuthority('PORTAIL_PARENT_ACCEDER','PORTAIL_PARENT_PEDAGOGIE','ROLE_PARENT')")
 public List<Map<String,Object>> parent(){return parentQuery(null);}

 /** Per-child endpoint; ownership is enforced in SQL, never trusted from a client-supplied ID. */
 @GetMapping("/parent/children/{studentId}")
 @PreAuthorize("hasAnyAuthority('PORTAIL_PARENT_ACCEDER','PORTAIL_PARENT_PEDAGOGIE','ROLE_PARENT')")
 public List<Map<String,Object>> parentChild(@PathVariable long studentId){return parentQuery(studentId);}

 private List<Map<String,Object>> parentQuery(Long studentId){
  String sql="""
  SELECT DISTINCT st.id AS student_id,sp.first_name AS student_first_name,sp.last_name AS student_last_name,
   s.id AS schedule_id,s.title,s.assessment_type,s.term,cg.id AS class_group_id,cg.name AS class_name,
   e.id AS entry_id,e.assessment_date,e.start_time,e.end_time,su.id AS subject_id,su.name AS subject_name,e.room,e.instructions
  FROM user_account ua JOIN guardian g ON g.person_id=ua.person_id
  JOIN student_guardian sg ON sg.guardian_id=g.id AND sg.active=true
  JOIN student st ON st.id=sg.student_id JOIN person sp ON sp.id=st.person_id
  JOIN student_enrollment se ON se.student_id=st.id AND se.status='ACTIVE'
  JOIN class_group cg ON cg.id=se.current_class_group_id
  JOIN assessment_schedule s ON s.class_group_id=cg.id AND s.school_year_id=se.school_year_id AND s.status='PUBLISHED'
  JOIN assessment_schedule_entry e ON e.schedule_id=s.id JOIN subject su ON su.id=e.subject_id
  WHERE ua.username=?
  """;
  List<Object> args=new ArrayList<>();args.add(username());
  if(studentId!=null){sql+=" AND st.id=?";args.add(studentId);}
  sql+=" ORDER BY st.id,e.assessment_date,e.start_time,e.id";
  return db.queryForList(sql,args.toArray());
 }
}
