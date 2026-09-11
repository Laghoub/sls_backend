package com.ecole.gestion_scolaire.finance.entity;

import jakarta.persistence.*;

import java.time.*;
import java.math.BigDecimal;

@Entity
@Table(name = "student_charge")
public class StudentCharge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "student_enrollment_id")
    private Long studentEnrollmentId;
    @Column(name = "registration_case_id")
    private Long registrationCaseId;
    @Column(name = "fee_type_id", nullable = false)
    private Long feeTypeId;
    @Column(name = "tariff_id")
    private Long tariffId;
    @Column(nullable = false, length = 200)
    private String label;
    @Column(name = "original_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal originalAmount;
    @Column(name = "discount_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal discountAmount;
    @Column(name = "final_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal finalAmount;
    @Column(name = "due_date")
    private LocalDate dueDate;
    @Column(name = "billing_period_start")
    private LocalDate billingPeriodStart;
    @Column(name = "billing_period_end")
    private LocalDate billingPeriodEnd;
    @Column(nullable = false, length = 30)
    private String status;
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;
    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @PrePersist
    void pre() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getStudentEnrollmentId() {
        return studentEnrollmentId;
    }

    public void setStudentEnrollmentId(Long v) {
        studentEnrollmentId = v;
    }

    public Long getRegistrationCaseId() {
        return registrationCaseId;
    }

    public void setRegistrationCaseId(Long v) {
        registrationCaseId = v;
    }

    public Long getFeeTypeId() {
        return feeTypeId;
    }

    public void setFeeTypeId(Long v) {
        feeTypeId = v;
    }

    public Long getTariffId() {
        return tariffId;
    }

    public void setTariffId(Long v) {
        tariffId = v;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String v) {
        label = v;
    }

    public BigDecimal getOriginalAmount() {
        return originalAmount;
    }

    public void setOriginalAmount(BigDecimal v) {
        originalAmount = v;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal v) {
        discountAmount = v;
    }

    public BigDecimal getFinalAmount() {
        return finalAmount;
    }

    public void setFinalAmount(BigDecimal v) {
        finalAmount = v;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate v) {
        dueDate = v;
    }

    public LocalDate getBillingPeriodStart() {
        return billingPeriodStart;
    }

    public void setBillingPeriodStart(LocalDate v) {
        billingPeriodStart = v;
    }

    public LocalDate getBillingPeriodEnd() {
        return billingPeriodEnd;
    }

    public void setBillingPeriodEnd(LocalDate v) {
        billingPeriodEnd = v;
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

    public OffsetDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(OffsetDateTime v) {
        cancelledAt = v;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String v) {
        cancellationReason = v;
    }
}
