package com.ecole.gestion_scolaire.finance.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "family_credit_usage")
public class FamilyCreditUsage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "family_credit_id", nullable = false)
    private Long familyCreditId;

    @Column(name = "guardian_id", nullable = false)
    private Long guardianId;

    @Column(name = "student_charge_id", nullable = false)
    private Long studentChargeId;

    @Column(name = "payment_allocation_id")
    private Long paymentAllocationId;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public Long getFamilyCreditId() { return familyCreditId; }
    public void setFamilyCreditId(Long v) { familyCreditId = v; }
    public Long getGuardianId() { return guardianId; }
    public void setGuardianId(Long v) { guardianId = v; }
    public Long getStudentChargeId() { return studentChargeId; }
    public void setStudentChargeId(Long v) { studentChargeId = v; }
    public Long getPaymentAllocationId() { return paymentAllocationId; }
    public void setPaymentAllocationId(Long v) { paymentAllocationId = v; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal v) { amount = v; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
