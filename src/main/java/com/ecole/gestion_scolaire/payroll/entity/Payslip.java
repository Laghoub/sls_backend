package com.ecole.gestion_scolaire.payroll.entity;
import jakarta.persistence.*; import java.time.OffsetDateTime;
@Entity @Table(name="payslip") public class Payslip {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @OneToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="payroll_id") private Payroll payroll;
 @Column(name="payslip_number",nullable=false,length=60,unique=true) private String payslipNumber;
 @Column(name="generated_at",nullable=false) private OffsetDateTime generatedAt;
 @Column(name="generated_by",nullable=false) private Long generatedBy;
 @Column(nullable=false,length=30) private String status;
 @PrePersist void pre(){if(generatedAt==null)generatedAt=OffsetDateTime.now();if(status==null)status="GENERATED";}
 public Long getId(){return id;} public Payroll getPayroll(){return payroll;} public void setPayroll(Payroll v){payroll=v;} public String getPayslipNumber(){return payslipNumber;} public void setPayslipNumber(String v){payslipNumber=v;} public OffsetDateTime getGeneratedAt(){return generatedAt;} public Long getGeneratedBy(){return generatedBy;} public void setGeneratedBy(Long v){generatedBy=v;} public String getStatus(){return status;} public void setStatus(String v){status=v;}
}
