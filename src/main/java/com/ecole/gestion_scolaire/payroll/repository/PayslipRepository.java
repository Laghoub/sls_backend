package com.ecole.gestion_scolaire.payroll.repository; import com.ecole.gestion_scolaire.payroll.entity.Payslip; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface PayslipRepository extends JpaRepository<Payslip,Long>{Optional<Payslip> findByPayrollId(Long payrollId);}
