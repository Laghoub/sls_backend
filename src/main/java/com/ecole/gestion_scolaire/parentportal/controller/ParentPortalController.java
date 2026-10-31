package com.ecole.gestion_scolaire.parentportal.controller;
import com.ecole.gestion_scolaire.assessmentplanning.AssessmentPlanningPortalController; import com.ecole.gestion_scolaire.parentportal.dto.ParentPortalDtos.*; import com.ecole.gestion_scolaire.parentportal.service.ParentPortalService; import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.web.bind.annotation.*; import java.util.*;
@RestController @RequestMapping("/api/parent-portal") @PreAuthorize("hasAuthority('PORTAIL_PARENT_ACCEDER')") public class ParentPortalController {private final ParentPortalService s; private final AssessmentPlanningPortalController planning;public ParentPortalController(ParentPortalService s,AssessmentPlanningPortalController planning){this.s=s;this.planning=planning;}
 @GetMapping("/me") public Me me(){return s.me();}@GetMapping("/dashboard") public Dashboard dashboard(){return s.dashboard();}
 @GetMapping("/children") @PreAuthorize("hasAuthority('PORTAIL_PARENT_ENFANTS')") public List<Child> children(){return s.children();}
 @GetMapping("/children/{id}/finance") @PreAuthorize("hasAuthority('PORTAIL_PARENT_FINANCES')") public FinanceSummary finance(@PathVariable Long id){return s.finance(id);}
 @GetMapping("/children/{id}/charges") @PreAuthorize("hasAuthority('PORTAIL_PARENT_FINANCES')") public List<Charge> charges(@PathVariable Long id){return s.charges(id);}
 @GetMapping("/payments") @PreAuthorize("hasAuthority('PORTAIL_PARENT_FINANCES')") public List<PaymentItem> payments(){return s.payments();}
 @GetMapping("/children/{id}/attendance") @PreAuthorize("hasAuthority('PORTAIL_PARENT_PEDAGOGIE')") public List<AttendanceItem> attendance(@PathVariable Long id){return s.attendance(id);}
 @GetMapping("/children/{id}/timetable") @PreAuthorize("hasAuthority('PORTAIL_PARENT_PEDAGOGIE')") public List<TimetableItem> timetable(@PathVariable Long id){return s.timetable(id);}
 @GetMapping("/children/{id}/assessment-planning") @PreAuthorize("hasAuthority('PORTAIL_PARENT_PEDAGOGIE')") public List<Map<String,Object>> assessmentPlanning(@PathVariable Long id){return planning.parentChild(id);}
 @GetMapping("/children/{id}/exercises") @PreAuthorize("hasAuthority('PORTAIL_PARENT_PEDAGOGIE')") public List<ExerciseItem> exercises(@PathVariable Long id){return s.exercises(id);}
 @GetMapping("/children/{id}/grades") @PreAuthorize("hasAuthority('PORTAIL_PARENT_PEDAGOGIE')") public List<GradeItem> grades(@PathVariable Long id){return s.grades(id);}
}
