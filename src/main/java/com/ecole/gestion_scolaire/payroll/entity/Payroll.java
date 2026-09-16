package com.ecole.gestion_scolaire.payroll.entity;

import com.ecole.gestion_scolaire.hr.entity.*;
import com.ecole.gestion_scolaire.hr.enums.CompensationType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity @Table(name="payroll")
public class Payroll {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="employee_id") private Employee employee;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="payroll_period_id") private PayrollPeriod payrollPeriod;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="compensation_plan_id") private CompensationPlan compensationPlan;
 @Column(name="version_number",nullable=false) private Integer versionNumber;
 @Enumerated(EnumType.STRING) @Column(name="compensation_type",nullable=false,length=30) private CompensationType compensationType;
 @Column(name="base_salary",nullable=false) private BigDecimal baseSalary=BigDecimal.ZERO;
 @Column(name="fixed_part",nullable=false) private BigDecimal fixedPart=BigDecimal.ZERO;
 @Column(name="hourly_rate") private BigDecimal hourlyRate;
 @Column(name="theoretical_days") private BigDecimal theoreticalDays;
 @Column(name="worked_days") private BigDecimal workedDays;
 @Column(name="absent_days") private BigDecimal absentDays;
 @Column(name="regular_hours",nullable=false) private BigDecimal regularHours=BigDecimal.ZERO;
 @Column(name="replacement_hours",nullable=false) private BigDecimal replacementHours=BigDecimal.ZERO;
 @Column(name="extra_hours",nullable=false) private BigDecimal extraHours=BigDecimal.ZERO;
 @Column(name="hourly_amount",nullable=false) private BigDecimal hourlyAmount=BigDecimal.ZERO;
 @Column(name="absence_deduction",nullable=false) private BigDecimal absenceDeduction=BigDecimal.ZERO;
 @Column(name="bonus_total",nullable=false) private BigDecimal bonusTotal=BigDecimal.ZERO;
 @Column(name="deduction_total",nullable=false) private BigDecimal deductionTotal=BigDecimal.ZERO;
 @Column(name="reimbursement_total",nullable=false) private BigDecimal reimbursementTotal=BigDecimal.ZERO;
 @Column(name="gross_salary",nullable=false) private BigDecimal grossSalary=BigDecimal.ZERO;
 @Column(name="contributable_salary",nullable=false) private BigDecimal contributableSalary=BigDecimal.ZERO;
 @Column(name="employee_social_contribution",nullable=false) private BigDecimal employeeSocialContribution=BigDecimal.ZERO;
 @Column(name="employer_social_contribution",nullable=false) private BigDecimal employerSocialContribution=BigDecimal.ZERO;
 @Column(name="other_contributions",nullable=false) private BigDecimal otherContributions=BigDecimal.ZERO;
 @Column(name="taxable_amount",nullable=false) private BigDecimal taxableAmount=BigDecimal.ZERO;
 @Column(name="tax_amount",nullable=false) private BigDecimal taxAmount=BigDecimal.ZERO;
 @Column(name="net_salary",nullable=false) private BigDecimal netSalary=BigDecimal.ZERO;
 @Column(nullable=false,length=30) private String status="CALCULATED";
 @Column(name="calculated_at") private OffsetDateTime calculatedAt;
 @Column(name="calculated_by") private Long calculatedBy;
 @Column(name="validated_at") private OffsetDateTime validatedAt;
 @Column(name="validated_by") private Long validatedBy;
 @Column(name="paid_at") private OffsetDateTime paidAt;
 @Column(name="created_at",nullable=false) private OffsetDateTime createdAt;
 @Column(name="updated_at",nullable=false) private OffsetDateTime updatedAt;
 @PrePersist void pre(){var n=OffsetDateTime.now();if(createdAt==null)createdAt=n;updatedAt=n;if(calculatedAt==null)calculatedAt=n;} @PreUpdate void upd(){updatedAt=OffsetDateTime.now();}
 public Long getId(){return id;} public Employee getEmployee(){return employee;} public void setEmployee(Employee v){employee=v;} public PayrollPeriod getPayrollPeriod(){return payrollPeriod;} public void setPayrollPeriod(PayrollPeriod v){payrollPeriod=v;} public CompensationPlan getCompensationPlan(){return compensationPlan;} public void setCompensationPlan(CompensationPlan v){compensationPlan=v;}
 public Integer getVersionNumber(){return versionNumber;} public void setVersionNumber(Integer v){versionNumber=v;} public CompensationType getCompensationType(){return compensationType;} public void setCompensationType(CompensationType v){compensationType=v;}
 public BigDecimal getBaseSalary(){return baseSalary;} public void setBaseSalary(BigDecimal v){baseSalary=v;} public BigDecimal getFixedPart(){return fixedPart;} public void setFixedPart(BigDecimal v){fixedPart=v;} public BigDecimal getHourlyRate(){return hourlyRate;} public void setHourlyRate(BigDecimal v){hourlyRate=v;}
 public BigDecimal getTheoreticalDays(){return theoreticalDays;} public void setTheoreticalDays(BigDecimal v){theoreticalDays=v;} public BigDecimal getWorkedDays(){return workedDays;} public void setWorkedDays(BigDecimal v){workedDays=v;} public BigDecimal getAbsentDays(){return absentDays;} public void setAbsentDays(BigDecimal v){absentDays=v;}
 public BigDecimal getRegularHours(){return regularHours;} public void setRegularHours(BigDecimal v){regularHours=v;} public BigDecimal getReplacementHours(){return replacementHours;} public void setReplacementHours(BigDecimal v){replacementHours=v;} public BigDecimal getExtraHours(){return extraHours;} public void setExtraHours(BigDecimal v){extraHours=v;}
 public BigDecimal getHourlyAmount(){return hourlyAmount;} public void setHourlyAmount(BigDecimal v){hourlyAmount=v;} public BigDecimal getAbsenceDeduction(){return absenceDeduction;} public void setAbsenceDeduction(BigDecimal v){absenceDeduction=v;} public BigDecimal getBonusTotal(){return bonusTotal;} public void setBonusTotal(BigDecimal v){bonusTotal=v;} public BigDecimal getDeductionTotal(){return deductionTotal;} public void setDeductionTotal(BigDecimal v){deductionTotal=v;} public BigDecimal getReimbursementTotal(){return reimbursementTotal;} public void setReimbursementTotal(BigDecimal v){reimbursementTotal=v;}
 public BigDecimal getGrossSalary(){return grossSalary;} public void setGrossSalary(BigDecimal v){grossSalary=v;} public BigDecimal getContributableSalary(){return contributableSalary;} public void setContributableSalary(BigDecimal v){contributableSalary=v;} public BigDecimal getEmployeeSocialContribution(){return employeeSocialContribution;} public void setEmployeeSocialContribution(BigDecimal v){employeeSocialContribution=v;} public BigDecimal getEmployerSocialContribution(){return employerSocialContribution;} public void setEmployerSocialContribution(BigDecimal v){employerSocialContribution=v;} public BigDecimal getOtherContributions(){return otherContributions;} public void setOtherContributions(BigDecimal v){otherContributions=v;} public BigDecimal getTaxableAmount(){return taxableAmount;} public void setTaxableAmount(BigDecimal v){taxableAmount=v;} public BigDecimal getTaxAmount(){return taxAmount;} public void setTaxAmount(BigDecimal v){taxAmount=v;} public BigDecimal getNetSalary(){return netSalary;} public void setNetSalary(BigDecimal v){netSalary=v;}
 public String getStatus(){return status;} public void setStatus(String v){status=v;} public OffsetDateTime getCalculatedAt(){return calculatedAt;} public void setCalculatedAt(OffsetDateTime v){calculatedAt=v;} public Long getCalculatedBy(){return calculatedBy;} public void setCalculatedBy(Long v){calculatedBy=v;} public OffsetDateTime getValidatedAt(){return validatedAt;} public void setValidatedAt(OffsetDateTime v){validatedAt=v;} public Long getValidatedBy(){return validatedBy;} public void setValidatedBy(Long v){validatedBy=v;} public OffsetDateTime getPaidAt(){return paidAt;} public void setPaidAt(OffsetDateTime v){paidAt=v;} public OffsetDateTime getCreatedAt(){return createdAt;} public OffsetDateTime getUpdatedAt(){return updatedAt;}
}
