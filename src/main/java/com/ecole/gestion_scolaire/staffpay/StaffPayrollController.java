package com.ecole.gestion_scolaire.staffpay;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@RestController
@RequestMapping("/api/staff-payroll")
public class StaffPayrollController {
 private final JdbcTemplate db;
 private final com.ecole.gestion_scolaire.finance.service.CurrentFinanceAccountService current;
 public StaffPayrollController(JdbcTemplate db, com.ecole.gestion_scolaire.finance.service.CurrentFinanceAccountService current){this.db=db;this.current=current;}
 private void nonTeacher(Long employeeId){
  require(employeeId!=null && db.queryForObject("SELECT COUNT(*) FROM employee WHERE id=? AND NOT EXISTS (SELECT 1 FROM teacher WHERE teacher.employee_id=employee.id)",Integer.class,employeeId)>0,"Employé introuvable ou enseignant : utiliser la paie des enseignants.");
 }

 public record Contract(Long employeeId,String payType,BigDecimal baseRate,LocalDate effectiveFrom,LocalDate effectiveTo){}
 public record Attendance(Long employeeId,LocalDate workDate,String status,LocalTime checkIn,LocalTime checkOut,Integer workedMinutes,Boolean paid,String note){}
 public record Calculate(Long employeeId,LocalDate month,BigDecimal bonus,BigDecimal deduction,String note){}
 public record Payment(BigDecimal amount,LocalDate paidOn,String method,String reference,String note,Long cashRegisterSessionId){}
 private void require(boolean ok,String message){if(!ok)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
 private List<Map<String,Object>> rows(String sql,Object...args){return db.queryForList(sql,args);}
 @GetMapping("/contracts") @PreAuthorize("hasAuthority('PERSONNEL_PAIE_CONSULTER')")
 public List<Map<String,Object>> contracts(@RequestParam Long employeeId){nonTeacher(employeeId);return rows("SELECT * FROM staff_pay_contract WHERE employee_id=? ORDER BY effective_from DESC",employeeId);}
 @PostMapping("/contracts") @Transactional @PreAuthorize("hasAuthority('PERSONNEL_REMUNERATION_GERER')")
 public Map<String,Object> contract(@RequestBody Contract c){
  nonTeacher(c.employeeId());
  require(c.employeeId()!=null && c.effectiveFrom()!=null && c.baseRate()!=null && c.baseRate().signum()>=0,"Invalid contract");
  require(Set.of("MONTHLY","DAILY","HOURLY").contains(c.payType()),"Invalid pay type");
  require(c.effectiveTo()==null || !c.effectiveTo().isBefore(c.effectiveFrom()),"Invalid dates");
  require(db.queryForObject("SELECT count(*) FROM employee WHERE id=?",Integer.class,c.employeeId())>0,"Employee not found");
  require(db.queryForObject("SELECT count(*) FROM staff_pay_contract WHERE employee_id=? AND effective_from<=COALESCE(?,DATE '9999-12-31') AND COALESCE(effective_to,DATE '9999-12-31')>=?",Integer.class,c.employeeId(),c.effectiveTo(),c.effectiveFrom())==0,"Overlapping contract");
  Long id=db.queryForObject("INSERT INTO staff_pay_contract(employee_id,pay_type,base_rate,effective_from,effective_to) VALUES(?,?,?,?,?) RETURNING id",Long.class,c.employeeId(),c.payType(),c.baseRate(),c.effectiveFrom(),c.effectiveTo());
  return rows("SELECT * FROM staff_pay_contract WHERE id=?",id).get(0);
 }
 @GetMapping("/attendance") @PreAuthorize("hasAuthority('PERSONNEL_ASSIDUITE_CONSULTER')")
 public List<Map<String,Object>> attendance(@RequestParam LocalDate from,@RequestParam LocalDate to){require(!to.isBefore(from),"Invalid date range");return rows("SELECT a.* FROM staff_attendance a WHERE a.work_date BETWEEN ? AND ? AND NOT EXISTS (SELECT 1 FROM teacher t WHERE t.employee_id=a.employee_id) ORDER BY a.work_date,a.employee_id",from,to);}
 @GetMapping("/employees") @PreAuthorize("hasAnyAuthority('PERSONNEL_ASSIDUITE_CONSULTER','PERSONNEL_PAIE_CONSULTER')")
 public List<Map<String,Object>> employees(){return rows("SELECT e.id,e.employee_number,e.position_title,p.first_name,p.last_name FROM employee e JOIN person p ON p.id=e.person_id WHERE NOT EXISTS (SELECT 1 FROM teacher t WHERE t.employee_id=e.id) AND e.employment_status='ACTIVE' ORDER BY p.last_name,p.first_name");}
 @PutMapping("/attendance") @Transactional @PreAuthorize("hasAuthority('PERSONNEL_ASSIDUITE_GERER')")
 public Map<String,Object> mark(@RequestBody Attendance a){
  nonTeacher(a.employeeId());
  require(a.employeeId()!=null && a.workDate()!=null && a.status()!=null,"Missing attendance data");
  require(Set.of("PRESENT","LATE","ABSENT_EXCUSED","ABSENT_UNEXCUSED","LEAVE","SICK","REST").contains(a.status()),"Invalid status");
  require(a.workedMinutes()==null || a.workedMinutes()>=0,"Invalid minutes");
  require(db.queryForObject("SELECT count(*) FROM staff_pay_run WHERE employee_id=? AND pay_month=date_trunc('month',CAST(? AS date))::date AND status='APPROVED'",Integer.class,a.employeeId(),a.workDate())==0,"Approved payroll: attendance locked");
  db.update("INSERT INTO staff_attendance(employee_id,work_date,status,check_in,check_out,worked_minutes,paid,note) VALUES(?,?,?,?,?,?,?,?) ON CONFLICT(employee_id,work_date) DO UPDATE SET status=EXCLUDED.status,check_in=EXCLUDED.check_in,check_out=EXCLUDED.check_out,worked_minutes=EXCLUDED.worked_minutes,paid=EXCLUDED.paid,note=EXCLUDED.note,updated_at=now()",a.employeeId(),a.workDate(),a.status(),a.checkIn(),a.checkOut(),a.workedMinutes(),a.paid()==null?true:a.paid(),a.note());
  return rows("SELECT * FROM staff_attendance WHERE employee_id=? AND work_date=?",a.employeeId(),a.workDate()).get(0);
 }
 @GetMapping("/runs") @PreAuthorize("hasAuthority('PERSONNEL_PAIE_CONSULTER')")
 public List<Map<String,Object>> runs(@RequestParam LocalDate month){return rows("SELECT r.*,COALESCE((SELECT SUM(p.amount) FROM staff_pay_payment p WHERE p.pay_run_id=r.id),0) AS paid_amount,r.net_amount-COALESCE((SELECT SUM(p.amount) FROM staff_pay_payment p WHERE p.pay_run_id=r.id),0) AS remaining FROM staff_pay_run r WHERE pay_month=? AND NOT EXISTS (SELECT 1 FROM teacher t WHERE t.employee_id=r.employee_id) ORDER BY employee_id",month.withDayOfMonth(1));}
 @PostMapping("/runs/calculate") @Transactional @PreAuthorize("hasAuthority('PERSONNEL_PAIE_CALCULER')")
 public Map<String,Object> calculate(@RequestBody Calculate q){
  nonTeacher(q.employeeId());
  require(q.employeeId()!=null && q.month()!=null,"Missing employee or month");
  LocalDate month=q.month().withDayOfMonth(1),end=month.plusMonths(1).minusDays(1);
  var contracts=rows("SELECT * FROM staff_pay_contract WHERE employee_id=? AND effective_from<=? AND (effective_to IS NULL OR effective_to>=?) ORDER BY effective_from DESC LIMIT 1",q.employeeId(),month,month);
  require(!contracts.isEmpty(),"No contract covering full month");
  var c=contracts.get(0);
  require(c.get("effective_to")==null || !((java.sql.Date)c.get("effective_to")).toLocalDate().isBefore(end),"Contract does not cover full month");
  BigDecimal rate=(BigDecimal)c.get("base_rate");String type=(String)c.get("pay_type");
  BigDecimal base;
  if(type.equals("MONTHLY"))base=rate;
  else if(type.equals("DAILY"))base=rate.multiply(BigDecimal.valueOf(db.queryForObject("SELECT count(*) FROM staff_attendance WHERE employee_id=? AND work_date BETWEEN ? AND ? AND paid=true AND status IN ('PRESENT','LATE','ABSENT_EXCUSED','LEAVE','SICK')",Long.class,q.employeeId(),month,end)));
  else base=rate.multiply(BigDecimal.valueOf(db.queryForObject("SELECT COALESCE(sum(worked_minutes),0) FROM staff_attendance WHERE employee_id=? AND work_date BETWEEN ? AND ? AND paid=true",Long.class,q.employeeId(),month,end))).divide(BigDecimal.valueOf(60),2,java.math.RoundingMode.HALF_UP);
  BigDecimal bonus=q.bonus()==null?BigDecimal.ZERO:q.bonus(),deduction=q.deduction()==null?BigDecimal.ZERO:q.deduction();
  require(bonus.signum()>=0 && deduction.signum()>=0 && base.add(bonus).compareTo(deduction)>=0,"Invalid adjustment");
  require(db.queryForObject("SELECT count(*) FROM staff_pay_run WHERE employee_id=? AND pay_month=? AND status<>'DRAFT'",Integer.class,q.employeeId(),month)==0,"Payroll already approved or cancelled");
  require(db.queryForObject("SELECT count(*) FROM staff_pay_payment p JOIN staff_pay_run r ON r.id=p.pay_run_id WHERE r.employee_id=? AND r.pay_month=?",Integer.class,q.employeeId(),month)==0,"Payroll has payments");
  Long id=db.queryForObject("INSERT INTO staff_pay_run(employee_id,pay_month,contract_id,base_amount,bonus_amount,deduction_amount,net_amount,note) VALUES(?,?,?,?,?,?,?,?) ON CONFLICT(employee_id,pay_month) DO UPDATE SET contract_id=EXCLUDED.contract_id,base_amount=EXCLUDED.base_amount,bonus_amount=EXCLUDED.bonus_amount,deduction_amount=EXCLUDED.deduction_amount,net_amount=EXCLUDED.net_amount,note=EXCLUDED.note RETURNING id",Long.class,q.employeeId(),month,c.get("id"),base,bonus,deduction,base.add(bonus).subtract(deduction),q.note());
  return rows("SELECT * FROM staff_pay_run WHERE id=?",id).get(0);
 }
 @PostMapping("/runs/{id}/approve") @Transactional @PreAuthorize("hasAuthority('PERSONNEL_PAIE_VALIDER')")
 public Map<String,Object> approve(@PathVariable Long id){require(db.queryForObject("SELECT count(*) FROM staff_pay_run r WHERE r.id=? AND NOT EXISTS (SELECT 1 FROM teacher t WHERE t.employee_id=r.employee_id)",Integer.class,id)>0,"Invalid employee");require(db.update("UPDATE staff_pay_run SET status='APPROVED',approved_at=now() WHERE id=? AND status='DRAFT'",id)==1,"Draft payroll not found");return rows("SELECT * FROM staff_pay_run WHERE id=?",id).get(0);}
 @PostMapping("/runs/{id}/payments") @Transactional @PreAuthorize("hasAuthority('PERSONNEL_PAIE_PAYER')")
 public Map<String,Object> pay(@PathVariable Long id,@RequestBody Payment p){
  require(p.amount()!=null && p.amount().signum()>0 && p.paidOn()!=null && p.method()!=null && !p.method().isBlank(),"Invalid payment");
  var r=rows("SELECT * FROM staff_pay_run WHERE id=? FOR UPDATE",id);require(!r.isEmpty() && "APPROVED".equals(r.get(0).get("status")),"Payroll not approved");
  Long employeeId=((Number)r.get(0).get("employee_id")).longValue();nonTeacher(employeeId);
  BigDecimal already=db.queryForObject("SELECT COALESCE(SUM(amount),0) FROM staff_pay_payment WHERE pay_run_id=?",BigDecimal.class,id);
  require(already.add(p.amount()).compareTo((BigDecimal)r.get(0).get("net_amount"))<=0,"Payment exceeds balance");
  // Cash payments require an open cashier-owned session; other payment methods do not affect cash.
  boolean cash=Set.of("CASH","ESPECES","ESPÈCES").contains(p.method().trim().toUpperCase(java.util.Locale.ROOT));
  Long sessionId=p.cashRegisterSessionId();
  if(cash){
   if(sessionId==null){
    var open=rows("SELECT id FROM cash_register_session WHERE cashier_id=? AND status='OPEN' ORDER BY opened_at DESC LIMIT 1",current.id());
    require(!open.isEmpty(),"Une session de caisse ouverte est obligatoire");sessionId=((Number)open.get(0).get("id")).longValue();
   }
   require(db.queryForObject("SELECT COUNT(*) FROM cash_register_session WHERE id=? AND cashier_id=? AND status='OPEN'",Integer.class,sessionId,current.id())==1,"Session de caisse non autorisée ou fermée");
  }
  Long paymentId=db.queryForObject("INSERT INTO staff_pay_payment(pay_run_id,amount,paid_on,payment_method,reference,note,cash_register_session_id,created_by) VALUES(?,?,?,?,?,?,?,?) RETURNING id",Long.class,id,p.amount(),p.paidOn(),p.method(),p.reference(),p.note(),sessionId,current.id());
  String generatedRef=(p.reference()==null || p.reference().isBlank()) ? String.format("EMP-%d-%08d",p.paidOn().getYear(),paymentId) : p.reference().trim();
  db.update("UPDATE staff_pay_payment SET reference=? WHERE id=?",generatedRef,paymentId);
  if(cash){
   db.update("INSERT INTO cash_movement(cash_register_session_id,movement_type,direction,amount,reference,description,created_by,created_at,staff_pay_payment_id) VALUES(?,'STAFF_SALARY','OUT',?,?,?,?,now(),?)",sessionId,p.amount(),generatedRef,"Paie personnel - employé " + employeeId,current.id(),paymentId);
  }
  return receipt(paymentId);
 }
 @GetMapping(value="/payments/{paymentId}/receipt.pdf",produces="application/pdf")
 @PreAuthorize("hasAuthority('PERSONNEL_PAIE_CONSULTER')")
 public org.springframework.http.ResponseEntity<byte[]> receiptPdf(@PathVariable Long paymentId) throws java.io.IOException {
  Map<String,Object> r=receipt(paymentId);
  byte[] pdf=com.ecole.gestion_scolaire.payroll.service.SharedSalaryReceiptPdf.render(
    "PERSONNEL",String.valueOf(r.get("receipt_number")),
    String.valueOf(r.get("first_name"))+" "+String.valueOf(r.get("last_name")),
    String.valueOf(r.get("employee_number")),String.valueOf(r.get("pay_month")),
    String.valueOf(r.get("paid_on")),String.valueOf(r.get("payment_method")),
    String.valueOf(r.get("reference")),String.valueOf(r.get("net_amount")),
    String.valueOf(r.get("amount")),String.valueOf(r.get("paid_total")),String.valueOf(r.get("remaining")));
  return org.springframework.http.ResponseEntity.ok().header("Content-Disposition","inline; filename=staff-receipt-"+paymentId+".pdf").body(pdf);
 }
 @GetMapping("/payments/{paymentId}/receipt") @PreAuthorize("hasAuthority('PERSONNEL_PAIE_CONSULTER')")
 public Map<String,Object> receipt(@PathVariable Long paymentId){
  var list=rows("SELECT p.id AS payment_id,p.amount,p.paid_on,p.payment_method,p.reference,r.id AS pay_run_id,r.pay_month,r.base_amount,r.bonus_amount,r.deduction_amount,r.net_amount,e.employee_number,e.position_title,pe.first_name,pe.last_name,(SELECT COALESCE(SUM(x.amount),0) FROM staff_pay_payment x WHERE x.pay_run_id=r.id) AS paid_total FROM staff_pay_payment p JOIN staff_pay_run r ON r.id=p.pay_run_id JOIN employee e ON e.id=r.employee_id JOIN person pe ON pe.id=e.person_id WHERE p.id=? AND NOT EXISTS(SELECT 1 FROM teacher t WHERE t.employee_id=e.id)",paymentId);
  require(!list.isEmpty(),"Reçu introuvable");
  var result=new LinkedHashMap<String,Object>(list.get(0));
  result.put("receipt_number",String.format("STAFF-%08d",paymentId));
  result.put("remaining",((BigDecimal)result.get("net_amount")).subtract((BigDecimal)result.get("paid_total")).max(BigDecimal.ZERO));
  return result;
 }
}
