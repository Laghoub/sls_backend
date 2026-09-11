package com.ecole.gestion_scolaire.finance.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "refund")
public class Refund {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "payment_id", nullable = false)
    private Long paymentId;
    @Column(name = "refund_date", nullable = false)
    private OffsetDateTime refundDate;
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;
    @Column(name = "payment_method_id", nullable = false)
    private Long paymentMethodId;
    @Column(nullable = false, columnDefinition = "text")
    private String reason;
    @Column(nullable = false, length = 30)
    private String status;
    @Column(name = "created_by", nullable = false)
    private Long createdBy;
    @Column(name = "validated_by")
    private Long validatedBy;
    @Column(name = "validated_at")
    private OffsetDateTime validatedAt;
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void pre() {
        var n = OffsetDateTime.now();
        if (createdAt == null) createdAt = n;
        if (refundDate == null) refundDate = n;
    }

    public Long getId() {
        return id;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long v) {
        paymentId = v;
    }

    public OffsetDateTime getRefundDate() {
        return refundDate;
    }

    public void setRefundDate(OffsetDateTime v) {
        refundDate = v;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal v) {
        amount = v;
    }

    public Long getPaymentMethodId() {
        return paymentMethodId;
    }

    public void setPaymentMethodId(Long v) {
        paymentMethodId = v;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String v) {
        reason = v;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String v) {
        status = v;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long v) {
        createdBy = v;
    }

    public Long getValidatedBy() {
        return validatedBy;
    }

    public void setValidatedBy(Long v) {
        validatedBy = v;
    }

    public OffsetDateTime getValidatedAt() {
        return validatedAt;
    }

    public void setValidatedAt(OffsetDateTime v) {
        validatedAt = v;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
