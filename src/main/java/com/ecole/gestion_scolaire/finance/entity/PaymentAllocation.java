package com.ecole.gestion_scolaire.finance.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "payment_allocation")
public class PaymentAllocation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "payment_id", nullable = false)
    private Long paymentId;
    @Column(name = "student_charge_id", nullable = false)
    private Long studentChargeId;
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void pre() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
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

    public Long getStudentChargeId() {
        return studentChargeId;
    }

    public void setStudentChargeId(Long v) {
        studentChargeId = v;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal v) {
        amount = v;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
