package com.ecole.gestion_scolaire.finance.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "cash_movement")
public class CashMovement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "cash_register_session_id", nullable = false)
    private Long cashRegisterSessionId;
    @Column(name = "movement_type", nullable = false, length = 40)
    private String movementType;
    @Column(nullable = false, length = 10)
    private String direction;
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;
    @Column(name = "payment_id")
    private Long paymentId;
    @Column(name = "refund_id")
    private Long refundId;
    @Column(name = "salary_payment_id")
    private Long salaryPaymentId;
    @Column(name = "provider_payment_id")
    private Long providerPaymentId;
    @Column(length = 150)
    private String reference;
    @Column(columnDefinition = "text")
    private String description;
    @Column(name = "created_by", nullable = false)
    private Long createdBy;
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void pre() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getCashRegisterSessionId() {
        return cashRegisterSessionId;
    }

    public void setCashRegisterSessionId(Long v) {
        cashRegisterSessionId = v;
    }

    public String getMovementType() {
        return movementType;
    }

    public void setMovementType(String v) {
        movementType = v;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String v) {
        direction = v;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal v) {
        amount = v;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long v) {
        paymentId = v;
    }

    public Long getRefundId() {
        return refundId;
    }

    public void setRefundId(Long v) {
        refundId = v;
    }

    public Long getSalaryPaymentId() {
        return salaryPaymentId;
    }

    public void setSalaryPaymentId(Long v) {
        salaryPaymentId = v;
    }

    public Long getProviderPaymentId() {
        return providerPaymentId;
    }

    public void setProviderPaymentId(Long v) {
        providerPaymentId = v;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String v) {
        reference = v;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String v) {
        description = v;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long v) {
        createdBy = v;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
