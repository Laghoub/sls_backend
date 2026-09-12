package com.ecole.gestion_scolaire.finance.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "refund_credit_reversal")
public class RefundCreditReversal {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "refund_id", nullable = false) private Long refundId;
    @Column(name = "family_credit_id", nullable = false) private Long familyCreditId;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal amount;
    @Column(name = "created_at", nullable = false) private OffsetDateTime createdAt;
    @PrePersist void pre(){if(createdAt==null)createdAt=OffsetDateTime.now();}
    public Long getId(){return id;} public Long getRefundId(){return refundId;} public void setRefundId(Long v){refundId=v;}
    public Long getFamilyCreditId(){return familyCreditId;} public void setFamilyCreditId(Long v){familyCreditId=v;}
    public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal v){amount=v;}
    public OffsetDateTime getCreatedAt(){return createdAt;}
}
