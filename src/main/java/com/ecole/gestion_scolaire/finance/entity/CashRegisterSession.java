package com.ecole.gestion_scolaire.finance.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "cash_register_session")
public class CashRegisterSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "cash_register_id", nullable = false)
    private Long cashRegisterId;
    @Column(name = "cashier_id", nullable = false)
    private Long cashierId;
    @Column(name = "opened_at", nullable = false)
    private OffsetDateTime openedAt;
    @Column(name = "opening_balance", nullable = false, precision = 14, scale = 2)
    private BigDecimal openingBalance;
    @Column(name = "closed_at")
    private OffsetDateTime closedAt;
    @Column(name = "expected_closing_balance", precision = 14, scale = 2)
    private BigDecimal expectedClosingBalance;
    @Column(name = "actual_closing_balance", precision = 14, scale = 2)
    private BigDecimal actualClosingBalance;
    @Column(name = "difference_amount", precision = 14, scale = 2)
    private BigDecimal differenceAmount;
    @Column(nullable = false, length = 30)
    private String status;
    @Column(name = "closed_by")
    private Long closedBy;

    public Long getId() {
        return id;
    }

    public Long getCashRegisterId() {
        return cashRegisterId;
    }

    public void setCashRegisterId(Long v) {
        cashRegisterId = v;
    }

    public Long getCashierId() {
        return cashierId;
    }

    public void setCashierId(Long v) {
        cashierId = v;
    }

    public OffsetDateTime getOpenedAt() {
        return openedAt;
    }

    public void setOpenedAt(OffsetDateTime v) {
        openedAt = v;
    }

    public BigDecimal getOpeningBalance() {
        return openingBalance;
    }

    public void setOpeningBalance(BigDecimal v) {
        openingBalance = v;
    }

    public OffsetDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(OffsetDateTime v) {
        closedAt = v;
    }

    public BigDecimal getExpectedClosingBalance() {
        return expectedClosingBalance;
    }

    public void setExpectedClosingBalance(BigDecimal v) {
        expectedClosingBalance = v;
    }

    public BigDecimal getActualClosingBalance() {
        return actualClosingBalance;
    }

    public void setActualClosingBalance(BigDecimal v) {
        actualClosingBalance = v;
    }

    public BigDecimal getDifferenceAmount() {
        return differenceAmount;
    }

    public void setDifferenceAmount(BigDecimal v) {
        differenceAmount = v;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String v) {
        status = v;
    }

    public Long getClosedBy() {
        return closedBy;
    }

    public void setClosedBy(Long v) {
        closedBy = v;
    }
}
