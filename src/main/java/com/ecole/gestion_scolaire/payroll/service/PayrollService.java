package com.ecole.gestion_scolaire.payroll.service;

import com.ecole.gestion_scolaire.common.exception.*;
import com.ecole.gestion_scolaire.finance.entity.*;
import com.ecole.gestion_scolaire.finance.repository.*;
import com.ecole.gestion_scolaire.hr.entity.*;
import com.ecole.gestion_scolaire.hr.enums.*;
import com.ecole.gestion_scolaire.hr.repository.*;
import com.ecole.gestion_scolaire.payroll.dto.*;
import com.ecole.gestion_scolaire.payroll.entity.*;
import com.ecole.gestion_scolaire.payroll.repository.*;
import com.ecole.gestion_scolaire.school.repository.SchoolYearRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service @Transactional
public class PayrollService {
 private static final BigDecimal ZERO=BigDecimal.ZERO.setScale(2); private static final DateTimeFormatter TIME=DateTimeFormatter.ofPattern("HH'h'mm");
 private final PayrollPeriodRepository periods; private final PayrollRepository payrolls; private final SalaryPaymentRepository payments; private final PayslipRepository payslips;
 private final TeacherRepository teachers; private final CompensationPlanRepository plans; private final TeacherRateRepository rates; private final SchoolYearRepository years; private final PaymentMethodRepository paymentMethods; private final CashRegisterSessionRepository cashSessions; private final CashMovementRepository cashMovements; private final PayrollCurrentUserService current; private final PayrollEmailService emails; private final EntityManager em;
 public PayrollService(PayrollPeriodRepository periods,PayrollRepository payrolls,SalaryPaymentRepository payments,PayslipRepository payslips,TeacherRepository teachers,CompensationPlanRepository plans,TeacherRateRepository rates,SchoolYearRepository years,PaymentMethodRepository paymentMethods,CashRegisterSessionRepository cashSessions,CashMovementRepository cashMovements,PayrollCurrentUserService current,PayrollEmailService emails,EntityManager em){this.periods=periods;this.payrolls=payrolls;this.payments=payments;this.payslips=payslips;this.teachers=teachers;this.plans=plans;this.rates=rates;this.years=years;this.paymentMethods=paymentMethods;this.cashSessions=cashSessions;this.cashMovements=cashMovements;this.current=current;this.emails=emails;this.em=em;}

 public PayrollResponse calculate(PayrollCalculateRequest q){
  var teacher=teachers.findById(q.teacherId()).orElseThrow(()->new ResourceNotFoundException("Enseignant introuvable")); var employee=teacher.getEmployee(); var year=years.findById(q.schoolYearId()).orElseThrow(()->new ResourceNotFoundException("Année scolaire introuvable"));
  LocalDate start=LocalDate.of(q.year(),q.month(),1), end=start.withDayOfMonth(start.lengthOfMonth()); if(end.isBefore(year.getStartDate())||start.isAfter(year.getEndDate()))throw new BusinessRuleException("La période demandée est hors de l’année scolaire sélectionnée.");
  var period=periods.findByYearAndMonth(q.year(),q.month()).orElseGet(()->{var p=new PayrollPeriod();p.setSchoolYear(year);p.setYear(q.year());p.setMonth(q.month());p.setStartDate(start);p.setEndDate(end);p.setStatus("OPEN");return periods.save(p);});
  if("CLOSED".equalsIgnoreCase(period.getStatus()))throw new BusinessRuleException("Cette période de paie est clôturée.");
  CompensationPlan plan=plans.findByEmployeeIdOrderByEffectiveFromDesc(employee.getId()).stream().filter(x->x.getStatus()==RecordStatus.ACTIVE&&(x.getSchoolYear()==null||Objects.equals(x.getSchoolYear().getId(),q.schoolYearId()))&&!x.getEffectiveFrom().isAfter(end)&&(x.getEffectiveUntil()==null||!x.getEffectiveUntil().isBefore(start))).findFirst().orElseThrow(()->new BusinessRuleException("Aucun plan de rémunération actif pour cet enseignant et cette période."));
  var previousVersions=payrolls.findByEmployeeIdAndPayrollPeriodIdOrderByVersionNumberDesc(employee.getId(),period.getId());
  if(previousVersions.stream().anyMatch(x->paid(x.getId()).signum()>0)) throw new BusinessRuleException("Une version de cette paie a déjà reçu un paiement. Elle ne peut plus être recalculée afin de préserver la traçabilité financière.");
  var previous=previousVersions.stream().findFirst().orElse(null);
  int version=previous==null?1:previous.getVersionNumber()+1;
  if(previous!=null && !"CANCELLED".equalsIgnoreCase(previous.getStatus())) { previous.setStatus("SUPERSEDED"); payrolls.save(previous); }
  var attendance=attendanceLines(teacher,q.schoolYearId(),start,end,plan);
  BigDecimal hours=attendance.stream().map(x->BigDecimal.valueOf(x.paidMinutes()).divide(BigDecimal.valueOf(60),4,RoundingMode.HALF_UP)).reduce(BigDecimal.ZERO,BigDecimal::add);
  BigDecimal hourlyAmount=attendance.stream().map(PayrollAttendanceLineResponse::amount).reduce(BigDecimal.ZERO,BigDecimal::add).setScale(2,RoundingMode.HALF_UP);
  boolean monthly=plan.getCompensationType()==CompensationType.MONTHLY||plan.getCompensationType()==CompensationType.MIXED;
  boolean hourly=plan.getCompensationType()==CompensationType.HOURLY||plan.getCompensationType()==CompensationType.MIXED;
  BigDecimal base=monthly?nvl(plan.getMonthlySalary()):ZERO; BigDecimal fixed=nvl(plan.getFixedPart()); if(!hourly){hours=BigDecimal.ZERO;hourlyAmount=ZERO;}
  var p=new Payroll();p.setEmployee(employee);p.setPayrollPeriod(period);p.setCompensationPlan(plan);p.setVersionNumber(version);p.setCompensationType(plan.getCompensationType());p.setBaseSalary(money(base));p.setFixedPart(money(fixed));p.setHourlyRate(plan.getHourlyRate());p.setRegularHours(hours.setScale(2,RoundingMode.HALF_UP));p.setReplacementHours(ZERO);p.setExtraHours(ZERO);p.setHourlyAmount(money(hourlyAmount));
  long present=attendance.stream().filter(x->"PRESENT".equals(x.attendanceStatus())).count(), absent=attendance.stream().filter(x->"ABSENT".equals(x.attendanceStatus())).count();
  BigDecimal absenceDeduction=money(q.absenceDeduction());
  if(absenceDeduction.signum()>0 && !monthly) throw new BusinessRuleException("La retenue manuelle pour absence est réservée aux rémunérations mensuelles ou mixtes.");
  if(absenceDeduction.signum()>0 && absent==0) throw new BusinessRuleException("Aucune absence n'est enregistrée sur cette période : aucune retenue d'absence ne peut être appliquée.");
  p.setTheoreticalDays(BigDecimal.valueOf(attendance.size()));p.setWorkedDays(BigDecimal.valueOf(attendance.size()-absent));p.setAbsentDays(BigDecimal.valueOf(absent));p.setAbsenceDeduction(absenceDeduction);p.setBonusTotal(ZERO);p.setDeductionTotal(absenceDeduction);p.setReimbursementTotal(ZERO);
  BigDecimal gross=base.add(fixed).add(hourly?hourlyAmount:ZERO);
  if(absenceDeduction.compareTo(gross)>0) throw new BusinessRuleException("La retenue d'absence ne peut pas dépasser la rémunération brute calculée.");
  BigDecimal net=gross.subtract(absenceDeduction);p.setGrossSalary(money(gross));p.setContributableSalary(money(gross));p.setEmployeeSocialContribution(ZERO);p.setEmployerSocialContribution(ZERO);p.setOtherContributions(ZERO);p.setTaxableAmount(money(net));p.setTaxAmount(ZERO);p.setNetSalary(money(net));p.setStatus("CALCULATED");p.setCalculatedAt(OffsetDateTime.now());p.setCalculatedBy(current.get().getId());p=payrolls.save(p);
  ensurePayslip(p); return map(p,teacher,attendance);
 }

 @Transactional(readOnly=true) public PayrollResponse get(Long id){var p=payrolls.findById(id).orElseThrow(()->new ResourceNotFoundException("Paie introuvable"));var t=teachers.findByEmployeeId(p.getEmployee().getId()).orElseThrow(()->new ResourceNotFoundException("Enseignant introuvable"));return map(p,t,attendanceLines(t,p.getPayrollPeriod().getSchoolYear()==null?null:p.getPayrollPeriod().getSchoolYear().getId(),p.getPayrollPeriod().getStartDate(),p.getPayrollPeriod().getEndDate(),p.getCompensationPlan()));}
 @Transactional(readOnly=true) public List<PayrollResponse> list(Long periodId){
  var all=payrolls.findByPayrollPeriodIdOrderByEmployeePersonLastNameAscEmployeePersonFirstNameAsc(periodId);
  Map<Long,List<Payroll>> grouped=new LinkedHashMap<>();
  for(var p:all) grouped.computeIfAbsent(p.getEmployee().getId(),k->new ArrayList<>()).add(p);
  Map<Long,Payroll> latest=new LinkedHashMap<>();
  for(var entry:grouped.entrySet()){var versions=entry.getValue();var paidVersion=versions.stream().filter(x->paid(x.getId()).signum()>0).max(Comparator.comparing(Payroll::getVersionNumber));var chosen=paidVersion.orElseGet(()->versions.stream().max(Comparator.comparing(Payroll::getVersionNumber)).orElseThrow());latest.put(entry.getKey(),chosen);}
  return latest.values().stream().sorted(Comparator.comparing((Payroll p)->p.getEmployee().getPerson().getLastName(),String.CASE_INSENSITIVE_ORDER).thenComparing(p->p.getEmployee().getPerson().getFirstName(),String.CASE_INSENSITIVE_ORDER)).map(p->{var t=teachers.findByEmployeeId(p.getEmployee().getId()).orElseThrow(()->new ResourceNotFoundException("Enseignant introuvable"));return map(p,t,List.of());}).toList();
 }
 @Transactional(readOnly=true) public List<PayrollPeriodResponse> periodList(){return periods.findAllByOrderByYearDescMonthDesc().stream().map(this::mapPeriod).toList();}

 public SalaryPaymentReceiptResponse pay(Long payrollId,SalaryPaymentRequest q){
  var p=payrolls.findById(payrollId).orElseThrow(()->new ResourceNotFoundException("Paie introuvable")); if("CANCELLED".equals(p.getStatus()))throw new BusinessRuleException("Cette paie est annulée.");
  BigDecimal already=paid(p.getId()), remaining=p.getNetSalary().subtract(already); if(remaining.signum()<=0)throw new BusinessRuleException("Cette paie est déjà intégralement réglée."); if(q.amount().compareTo(remaining)>0)throw new BusinessRuleException("Le montant dépasse le reste à payer : "+remaining+".");
  var method=paymentMethods.findById(q.paymentMethodId()).orElseThrow(()->new ResourceNotFoundException("Mode de paiement introuvable"));if(!method.isActive())throw new BusinessRuleException("Ce mode de paiement est inactif.");
  boolean cash="ESPECES".equalsIgnoreCase(method.getCode())||"CASH".equalsIgnoreCase(method.getCode()); Long sessionId=q.cashRegisterSessionId(); if(cash){if(sessionId==null)throw new BusinessRuleException("Une session de caisse ouverte est obligatoire pour un paiement en espèces.");var s=cashSessions.findById(sessionId).orElseThrow(()->new ResourceNotFoundException("Session de caisse introuvable"));if(!"OPEN".equalsIgnoreCase(s.getStatus()))throw new BusinessRuleException("La session de caisse n’est pas ouverte.");}
  var user=current.get();var sp=new SalaryPayment();sp.setPayroll(p);sp.setAmount(money(q.amount()));sp.setPaymentMethod(method);sp.setCashRegisterSessionId(sessionId);sp.setReference(blank(q.reference()));sp.setStatus("VALIDATED");sp.setPaidBy(user.getId());sp=payments.save(sp);
  if(cash){var m=new CashMovement();m.setCashRegisterSessionId(sessionId);m.setMovementType("SALARY_PAYMENT");m.setDirection("OUT");m.setAmount(sp.getAmount());m.setSalaryPaymentId(sp.getId());m.setReference(sp.getReference());m.setDescription("Paiement salaire - "+p.getEmployee().getEmployeeNumber());m.setCreatedBy(user.getId());cashMovements.save(m);}
  BigDecimal total=paid(p.getId());if(total.compareTo(p.getNetSalary())>=0){p.setStatus("PAID");p.setPaidAt(OffsetDateTime.now());}else p.setStatus("PARTIALLY_PAID");payrolls.save(p);
  var receipt=receipt(sp.getId());emails.queuePaymentReceipt(sp.getId());return receipt;
 }
 @Transactional(readOnly=true) public SalaryPaymentReceiptResponse receipt(Long salaryPaymentId){var sp=payments.findById(salaryPaymentId).orElseThrow(()->new ResourceNotFoundException("Paiement de salaire introuvable"));var p=sp.getPayroll();var slip=payslips.findByPayrollId(p.getId()).orElse(null);var person=p.getEmployee().getPerson();BigDecimal total=paid(p.getId()),remain=p.getNetSalary().subtract(total).max(BigDecimal.ZERO);return new SalaryPaymentReceiptResponse(sp.getId(),"PAY-"+String.format("%08d",sp.getId()),p.getId(),slip==null?null:slip.getPayslipNumber(),p.getEmployee().getEmployeeNumber(),person.getFirstName()+" "+person.getLastName(),person.getEmail(),p.getPayrollPeriod().getYear(),p.getPayrollPeriod().getMonth(),p.getPayrollPeriod().getStartDate(),p.getPayrollPeriod().getEndDate(),p.getCompensationType().name(),p.getBaseSalary(),p.getFixedPart(),p.getRegularHours(),p.getHourlyAmount(),p.getAbsenceDeduction(),p.getGrossSalary(),p.getNetSalary(),sp.getAmount(),total,remain,sp.getPaymentMethod().getName(),sp.getReference(),sp.getPaymentDate(),sp.getStatus());}

 private List<PayrollAttendanceLineResponse> attendanceLines(Teacher teacher,Long schoolYearId,LocalDate start,LocalDate end,CompensationPlan plan){
  @SuppressWarnings("unchecked") List<Object[]> rows=em.createNativeQuery("select ta.id,cs.session_date,cg.name,s.name,ts.start_time,ts.end_time,ta.attendance_status,coalesce(ta.late_minutes,0),cg.id,s.id from teacher_attendance ta join class_session cs on cs.id=ta.class_session_id join class_group cg on cg.id=cs.class_group_id join subject s on s.id=cs.subject_id join time_slot ts on ts.id=cs.time_slot_id where ta.teacher_id=:tid and ta.validation_status='VALIDATED' and cg.school_year_id=:syid and cs.session_date between :d1 and :d2 order by cs.session_date,ts.start_time").setParameter("tid",teacher.getId()).setParameter("syid",schoolYearId).setParameter("d1",start).setParameter("d2",end).getResultList();
  List<TeacherRate> teacherRates=rates.findByTeacherIdOrderByEffectiveFromDesc(teacher.getId());List<PayrollAttendanceLineResponse> out=new ArrayList<>();
  for(Object[] r:rows){Long id=((Number)r[0]).longValue();LocalDate d=asDate(r[1]);String cls=(String)r[2],sub=(String)r[3];LocalTime st=asTime(r[4]),et=asTime(r[5]);String status=(String)r[6];int late=((Number)r[7]).intValue();Long classId=((Number)r[8]).longValue(),subjectId=((Number)r[9]).longValue();int scheduled=(int)Duration.between(st,et).toMinutes();int paidMinutes="ABSENT".equals(status)?0:Math.max(0,scheduled-("RETARD".equals(status)?late:0));BigDecimal rate=resolveRate(teacherRates,schoolYearId,classId,subjectId,d,plan.getHourlyRate());BigDecimal amount=rate.multiply(BigDecimal.valueOf(paidMinutes)).divide(BigDecimal.valueOf(60),2,RoundingMode.HALF_UP);out.add(new PayrollAttendanceLineResponse(id,d,cls,sub,TIME.format(st)+" – "+TIME.format(et),status,late,scheduled,paidMinutes,money(rate),money(amount)));}
  return out;
 }
 private BigDecimal resolveRate(List<TeacherRate> all,Long schoolYearId,Long classId,Long subjectId,LocalDate date,BigDecimal fallback){return all.stream().filter(x->x.isActive()&&Objects.equals(x.getSchoolYear().getId(),schoolYearId)&&!x.getEffectiveFrom().isAfter(date)&&(x.getEffectiveUntil()==null||!x.getEffectiveUntil().isBefore(date))&&(x.getClassGroup()==null||Objects.equals(x.getClassGroup().getId(),classId))&&(x.getSubject()==null||Objects.equals(x.getSubject().getId(),subjectId))).sorted(Comparator.<TeacherRate>comparingInt(x->(x.getClassGroup()!=null?2:0)+(x.getSubject()!=null?1:0)).reversed().thenComparing(TeacherRate::getEffectiveFrom,Comparator.reverseOrder())).map(TeacherRate::getHourlyRate).findFirst().orElse(nvl(fallback));}
 private PayrollResponse map(Payroll p,Teacher teacher,List<PayrollAttendanceLineResponse> lines){var person=p.getEmployee().getPerson();long present=lines.stream().filter(x->"PRESENT".equals(x.attendanceStatus())).count(),absent=lines.stream().filter(x->"ABSENT".equals(x.attendanceStatus())).count(),late=lines.stream().filter(x->"RETARD".equals(x.attendanceStatus())).count();int lateMin=lines.stream().mapToInt(x->x.lateMinutes()==null?0:x.lateMinutes()).sum();BigDecimal paid=paid(p.getId()),remaining=p.getNetSalary().subtract(paid).max(BigDecimal.ZERO);return new PayrollResponse(p.getId(),teacher.getId(),p.getEmployee().getId(),p.getEmployee().getEmployeeNumber(),person.getFirstName()+" "+person.getLastName(),person.getEmail(),p.getPayrollPeriod().getId(),p.getPayrollPeriod().getYear(),p.getPayrollPeriod().getMonth(),p.getPayrollPeriod().getStartDate(),p.getPayrollPeriod().getEndDate(),p.getCompensationType().name(),p.getBaseSalary(),p.getFixedPart(),p.getHourlyRate(),present,absent,late,lateMin,p.getRegularHours(),p.getHourlyAmount(),p.getAbsenceDeduction(),p.getBonusTotal(),p.getDeductionTotal(),p.getReimbursementTotal(),p.getGrossSalary(),p.getNetSalary(),paid,remaining,p.getStatus(),p.getVersionNumber(),p.getCalculatedAt(),lines);}
 private PayrollPeriodResponse mapPeriod(PayrollPeriod p){return new PayrollPeriodResponse(p.getId(),p.getSchoolYear()==null?null:p.getSchoolYear().getId(),p.getSchoolYear()==null?null:p.getSchoolYear().getLabel(),p.getYear(),p.getMonth(),p.getStartDate(),p.getEndDate(),p.getStatus());}
 private Payslip ensurePayslip(Payroll p){return payslips.findByPayrollId(p.getId()).orElseGet(()->{var x=new Payslip();x.setPayroll(p);x.setPayslipNumber("BUL-"+p.getPayrollPeriod().getYear()+String.format("%02d",p.getPayrollPeriod().getMonth())+"-"+p.getEmployee().getEmployeeNumber()+"-V"+p.getVersionNumber());x.setGeneratedBy(current.get().getId());x.setStatus("GENERATED");return payslips.save(x);});}
 private LocalDate asDate(Object v){if(v instanceof LocalDate d)return d;if(v instanceof java.sql.Date d)return d.toLocalDate();return LocalDate.parse(v.toString());} private LocalTime asTime(Object v){if(v instanceof LocalTime t)return t;if(v instanceof java.sql.Time t)return t.toLocalTime();return LocalTime.parse(v.toString());}
 private BigDecimal paid(Long payrollId){return payments.findByPayrollIdOrderByPaymentDateDesc(payrollId).stream().filter(x->"VALIDATED".equalsIgnoreCase(x.getStatus())).map(SalaryPayment::getAmount).reduce(BigDecimal.ZERO,BigDecimal::add).setScale(2,RoundingMode.HALF_UP);} private BigDecimal nvl(BigDecimal x){return x==null?BigDecimal.ZERO:x;} private BigDecimal money(BigDecimal x){return nvl(x).setScale(2,RoundingMode.HALF_UP);} private String blank(String s){return s==null||s.isBlank()?null:s.trim();}
}
