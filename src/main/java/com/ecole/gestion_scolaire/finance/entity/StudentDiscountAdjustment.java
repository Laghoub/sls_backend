package com.ecole.gestion_scolaire.finance.entity;
import jakarta.persistence.*; import java.math.BigDecimal; import java.time.OffsetDateTime;
@Entity @Table(name="student_discount_adjustment")
public class StudentDiscountAdjustment {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="student_discount_id") private Long studentDiscountId; @Column(name="discount_rule_id") private Long discountRuleId;
 @Column(name="student_charge_id",nullable=false) private Long studentChargeId;
 @Column(name="previous_discount_amount",nullable=false,precision=14,scale=2) private BigDecimal previousDiscountAmount;
 @Column(name="new_discount_amount",nullable=false,precision=14,scale=2) private BigDecimal newDiscountAmount;
 @Column(name="previous_final_amount",nullable=false,precision=14,scale=2) private BigDecimal previousFinalAmount;
 @Column(name="new_final_amount",nullable=false,precision=14,scale=2) private BigDecimal newFinalAmount;
 @Column(name="credit_created",nullable=false,precision=14,scale=2) private BigDecimal creditCreated;
 @Column(name="applied_by",nullable=false) private Long appliedBy; @Column(name="created_at",nullable=false) private OffsetDateTime createdAt;
 @PrePersist void pre(){if(createdAt==null)createdAt=OffsetDateTime.now(); if(creditCreated==null)creditCreated=BigDecimal.ZERO;}
 public Long getId(){return id;} public void setStudentDiscountId(Long v){studentDiscountId=v;} public void setDiscountRuleId(Long v){discountRuleId=v;} public void setStudentChargeId(Long v){studentChargeId=v;}
 public void setPreviousDiscountAmount(BigDecimal v){previousDiscountAmount=v;} public void setNewDiscountAmount(BigDecimal v){newDiscountAmount=v;} public void setPreviousFinalAmount(BigDecimal v){previousFinalAmount=v;} public void setNewFinalAmount(BigDecimal v){newFinalAmount=v;} public void setCreditCreated(BigDecimal v){creditCreated=v;} public void setAppliedBy(Long v){appliedBy=v;}
}
