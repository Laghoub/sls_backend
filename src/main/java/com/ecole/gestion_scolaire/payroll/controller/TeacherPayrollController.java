package com.ecole.gestion_scolaire.payroll.controller;
import com.ecole.gestion_scolaire.payroll.dto.*; import com.ecole.gestion_scolaire.payroll.service.PayrollService; import jakarta.validation.Valid; import org.springframework.http.*; import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.web.bind.annotation.*; import java.util.*;
@RestController @RequestMapping("/api/payroll/teachers") public class TeacherPayrollController {
 private final PayrollService service; public TeacherPayrollController(PayrollService s){service=s;}
 @PostMapping("/calculate") @PreAuthorize("hasAuthority('PAIE_ENSEIGNANT_CALCULER')") public ResponseEntity<PayrollResponse> calculate(@Valid @RequestBody PayrollCalculateRequest q){return ResponseEntity.status(HttpStatus.CREATED).body(service.calculate(q));}
 @GetMapping("/{id}") @PreAuthorize("hasAuthority('PAIE_ENSEIGNANT_CONSULTER')") public PayrollResponse get(@PathVariable Long id){return service.get(id);}
 @GetMapping @PreAuthorize("hasAuthority('PAIE_ENSEIGNANT_CONSULTER')") public List<PayrollResponse> list(@RequestParam Long periodId){return service.list(periodId);}
 @GetMapping("/periods") @PreAuthorize("hasAuthority('PAIE_ENSEIGNANT_CONSULTER')") public List<PayrollPeriodResponse> periods(){return service.periodList();}
 @PostMapping("/{payrollId}/payments") @PreAuthorize("hasAuthority('PAIE_ENSEIGNANT_PAYER')") public ResponseEntity<SalaryPaymentReceiptResponse> pay(@PathVariable Long payrollId,@Valid @RequestBody SalaryPaymentRequest q){return ResponseEntity.status(HttpStatus.CREATED).body(service.pay(payrollId,q));}
 @GetMapping("/payments/{paymentId}/receipt") @PreAuthorize("hasAuthority('PAIE_ENSEIGNANT_CONSULTER')") public SalaryPaymentReceiptResponse receipt(@PathVariable Long paymentId){return service.receipt(paymentId);}
}
