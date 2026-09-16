package com.ecole.gestion_scolaire.payroll.repository; import com.ecole.gestion_scolaire.payroll.entity.SalaryPayment; import org.springframework.data.jpa.repository.JpaRepository; import java.math.BigDecimal; import java.util.*;
public interface SalaryPaymentRepository extends JpaRepository<SalaryPayment,Long>{List<SalaryPayment> findByPayrollIdOrderByPaymentDateDesc(Long payrollId);}
