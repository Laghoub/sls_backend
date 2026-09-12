package com.ecole.gestion_scolaire.finance.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(name="student_discount")
public class StudentDiscount {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="student_enrollment_id",nullable=false) private Long studentEnrollmentId;
 @Column(name="discount_rule_id") private Long discountRuleId;
 @Column(name="fee_type_id") private Long feeTypeId;
 @Column(name="discount_type",nullable=false,length=30) private String discountType;
 @Column(nullable=false,precision=14,scale=2) private BigDecimal value;
 @Column(name="start_date",nullable=false) private LocalDate startDate;
 @Column(name="end_date") private LocalDate endDate;
 @Column(columnDefinition="text") private String reason;
 @Column(nullable=false,length=30) private String status;
 @Column(name="approved_by") private Long approvedBy; @Column(name="approved_at") private OffsetDateTime approvedAt;
 @Column(name="created_by",nullable=false) private Long createdBy; @Column(name="created_at",nullable=false) private OffsetDateTime createdAt;
 @Column(name="rejected_by") private Long rejectedBy; @Column(name="rejected_at") private OffsetDateTime rejectedAt;
 @Column(name="rejection_reason") private String rejectionReason; @Column(name="applied_at") private OffsetDateTime appliedAt;
 @PrePersist void pre(){if(createdAt==null)createdAt=OffsetDateTime.now();}
 public Long getId(){return id;} public Long getStudentEnrollmentId(){return studentEnrollmentId;} public void setStudentEnrollmentId(Long v){studentEnrollmentId=v;}
 public Long getDiscountRuleId(){return discountRuleId;} public void setDiscountRuleId(Long v){discountRuleId=v;} public Long getFeeTypeId(){return feeTypeId;} public void setFeeTypeId(Long v){feeTypeId=v;}
 public String getDiscountType(){return discountType;} public void setDiscountType(String v){discountType=v;} public BigDecimal getValue(){return value;} public void setValue(BigDecimal v){value=v;}
 public LocalDate getStartDate(){return startDate;} public void setStartDate(LocalDate v){startDate=v;} public LocalDate getEndDate(){return endDate;} public void setEndDate(LocalDate v){endDate=v;}
 public String getReason(){return reason;} public void setReason(String v){reason=v;} public String getStatus(){return status;} public void setStatus(String v){status=v;}
 public Long getApprovedBy(){return approvedBy;} public void setApprovedBy(Long v){approvedBy=v;} public OffsetDateTime getApprovedAt(){return approvedAt;} public void setApprovedAt(OffsetDateTime v){approvedAt=v;}
 public Long getCreatedBy(){return createdBy;} public void setCreatedBy(Long v){createdBy=v;} public OffsetDateTime getCreatedAt(){return createdAt;}
 public Long getRejectedBy(){return rejectedBy;} public void setRejectedBy(Long v){rejectedBy=v;} public OffsetDateTime getRejectedAt(){return rejectedAt;} public void setRejectedAt(OffsetDateTime v){rejectedAt=v;}
 public String getRejectionReason(){return rejectionReason;} public void setRejectionReason(String v){rejectionReason=v;} public OffsetDateTime getAppliedAt(){return appliedAt;} public void setAppliedAt(OffsetDateTime v){appliedAt=v;}
}
