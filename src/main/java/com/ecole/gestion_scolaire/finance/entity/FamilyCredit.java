package com.ecole.gestion_scolaire.finance.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "family_credit")
public class FamilyCredit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "guardian_id", nullable = false)
    private Long guardianId;
    @Column(name = "source_payment_id")
    private Long sourcePaymentId;
    @Column(name = "source_student_discount_id")
    private Long sourceStudentDiscountId;
    @Column(name = "source_type", nullable = false, length = 30)
    private String sourceType;
    @Column(name = "initial_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal initialAmount;
    @Column(name = "remaining_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal remainingAmount;
    @Column(nullable = false, length = 30)
    private String status;
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void pre() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
        if (sourceType == null) sourceType = sourcePaymentId == null ? "DISCOUNT" : "PAYMENT";
    }

    public Long getId() {
        return id;
    }

    public Long getGuardianId() {
        return guardianId;
    }

    public void setGuardianId(Long v) {
        guardianId = v;
    }

    public Long getSourcePaymentId() {
        return sourcePaymentId;
    }

    public void setSourcePaymentId(Long v) {
        sourcePaymentId = v;
    }

    public Long getSourceStudentDiscountId() { return sourceStudentDiscountId; }
    public void setSourceStudentDiscountId(Long v) { sourceStudentDiscountId = v; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String v) { sourceType = v; }

    public BigDecimal getInitialAmount() {
        return initialAmount;
    }

    public void setInitialAmount(BigDecimal v) {
        initialAmount = v;
    }

    public BigDecimal getRemainingAmount() {
        return remainingAmount;
    }

    public void setRemainingAmount(BigDecimal v) {
        remainingAmount = v;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String v) {
        status = v;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
