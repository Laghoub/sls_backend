package com.ecole.gestion_scolaire.attendance.monthly;
import com.ecole.gestion_scolaire.attendance.enums.*;
import com.ecole.gestion_scolaire.identity.service.SurveillantCycleScope;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
@Service
public class MonthlyPointageService {
 private final MonthlyPointageRepository repository;
 private final SurveillantCycleScope scope;
 public MonthlyPointageService(MonthlyPointageRepository repository, SurveillantCycleScope scope) {
  this.repository=repository; this.scope=scope;
 }
 @Transactional(readOnly=true)
 public List<MonthlyPointageRow> rows(LocalDate from, LocalDate to, Long schoolYearId,
     String cycle, Long classId, Long teacherId, Long subjectId) {
  if(from==null || to==null || to.isBefore(from) || from.plusMonths(12).isBefore(to))
   throw new IllegalArgumentException("Période invalide (maximum 12 mois)");
  String restricted=scope.restrictedCycle();
  if(restricted!=null) {
   if(cycle!=null && !cycle.equalsIgnoreCase(restricted))
    throw new AccessDeniedException("Cycle non autorisé");
   cycle=restricted;
  }
  List<MonthlyPointageRow> result=new ArrayList<>();
  for(var a:repository.rows(from,to,schoolYearId,cycle,classId,teacherId,subjectId)) {
   var s=a.getClassSession();
   long minutes=Duration.between(s.getTimeSlot().getStartTime(),s.getTimeSlot().getEndTime()).toMinutes();
   if(minutes<=0) continue;
   BigDecimal planned=BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60),2,java.math.RoundingMode.HALF_UP);
   BigDecimal performed=BigDecimal.ZERO;
   // Seuls les pointages validés comptent comme heures effectuées.
   if(a.getValidationStatus()==AttendanceValidationStatus.VALIDATED &&
      (a.getAttendanceStatus()==TeacherAttendanceStatus.PRESENT ||
       a.getAttendanceStatus()==TeacherAttendanceStatus.RETARD)) {
    // Les retards sont signalés, sans déduction automatique faute de règle salariale validée.
    performed=planned;
   }
   var person=s.getTeacher().getEmployee().getPerson();
   result.add(new MonthlyPointageRow(s.getTeacher().getId(),
    person.getFirstName()+" "+person.getLastName(),
    s.getClassGroup().getId(),s.getClassGroup().getName(),
    s.getSubject().getId(),s.getSubject().getName(),s.getSessionDate(),
    planned,performed,a.getAttendanceStatus().name()+"/"+a.getValidationStatus().name(),s.getId()));
  }
  return result;
 }
 @Transactional(readOnly=true)
 public List<MonthlyPointageSummary> summary(LocalDate from,LocalDate to,Long schoolYearId,
  String cycle,Long classId,Long teacherId,Long subjectId) {
  record Key(Long teacherId,String teacherName,Long classId,String className) {}
  Map<Key,Map<LocalDate,BigDecimal>> groups=new LinkedHashMap<>();
  for(var r:rows(from,to,schoolYearId,cycle,classId,teacherId,subjectId)) {
   var key=new Key(r.teacherId(),r.teacherName(),r.classId(),r.className());
   groups.computeIfAbsent(key,k->new TreeMap<>()).merge(r.date(),r.performedHours(),BigDecimal::add);
  }
  List<MonthlyPointageSummary> out=new ArrayList<>();
  groups.forEach((key,days)->out.add(new MonthlyPointageSummary(key.teacherId(),key.teacherName(),
    key.classId(),key.className(),days,
    days.values().stream().reduce(BigDecimal.ZERO,BigDecimal::add))));
  return out;
 }
}
