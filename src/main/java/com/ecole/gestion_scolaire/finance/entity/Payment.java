package com.ecole.gestion_scolaire.finance.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "payment")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "payment_number", nullable = false, length = 50)
    private String paymentNumber;
    @Column(name = "guardian_id", nullable = false)
    private Long guardianId;
    @Column(name = "payment_date", nullable = false)
    private OffsetDateTime paymentDate;
    @Column(name = "payment_method_id", nullable = false)
    private Long paymentMethodId;
    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;
    @Column(nullable = false, length = 30)
    private String status;
    @Column(name = "cashier_id", nullable = false)
    private Long cashierId;
    @Column(name = "cash_register_session_id")
    private Long cashRegisterSessionId;
    @Column(name = "external_reference", length = 150)
    private String externalReference;
    @Column(columnDefinition = "text")
    private String notes;
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
    @Column(name = "validated_at")
    private OffsetDateTime validatedAt;
    @Column(name = "validated_by")
    private Long validatedBy;
    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;
    @Column(name = "cancelled_by")
    private Long cancelledBy;
    @Column(name = "cancellation_reason", columnDefinition = "text")
    private String cancellationReason;

    @PrePersist
    void pre() {
        var n = OffsetDateTime.now();
        if (createdAt == null) createdAt = n;
        if (paymentDate == null) paymentDate = n;
    }

    public Long getId() {
        return id;
    }

    public String getPaymentNumber() {
        return paymentNumber;
    }

    public void setPaymentNumber(String v) {
        paymentNumber = v;
    }

    public Long getGuardianId() {
        return guardianId;
    }

    public void setGuardianId(Long v) {
        guardianId = v;
    }

    public OffsetDateTime getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(OffsetDateTime v) {
        paymentDate = v;
    }

    public Long getPaymentMethodId() {
        return paymentMethodId;
    }

    public void setPaymentMethodId(Long v) {
        paymentMethodId = v;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal v) {
        totalAmount = v;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String v) {
        status = v;
    }

    public Long getCashierId() {
        return cashierId;
    }

    public void setCashierId(Long v) {
        cashierId = v;
    }

    public Long getCashRegisterSessionId() {
        return cashRegisterSessionId;
    }

    public void setCashRegisterSessionId(Long v) {
        cashRegisterSessionId = v;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public void setExternalReference(String v) {
        externalReference = v;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String v) {
        notes = v;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getValidatedAt() {
        return validatedAt;
    }

    public void setValidatedAt(OffsetDateTime v) {
        validatedAt = v;
    }

    public Long getValidatedBy() {
        return validatedBy;
    }

    public void setValidatedBy(Long v) {
        validatedBy = v;
    }

    public OffsetDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(OffsetDateTime v) {
        cancelledAt = v;
    }

    public Long getCancelledBy() {
        return cancelledBy;
    }

    public void setCancelledBy(Long v) {
        cancelledBy = v;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String v) {
        cancellationReason = v;
    }
}
