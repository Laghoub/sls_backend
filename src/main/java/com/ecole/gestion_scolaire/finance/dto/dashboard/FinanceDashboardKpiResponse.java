package com.ecole.gestion_scolaire.finance.dto.dashboard;

import java.math.BigDecimal;

public record FinanceDashboardKpiResponse(
        BigDecimal originalCharged,
        BigDecimal discountsGranted,
        BigDecimal netCharged,
        BigDecimal collectedOnCharges,
        BigDecimal remainingToCollect,
        BigDecimal overdueAmount,
        BigDecimal grossPayments,
        BigDecimal refunds,
        BigDecimal netPayments,
        BigDecimal familyCreditAvailable,
        BigDecimal collectionRate,
        BigDecimal averagePayment,
        long chargeCount,
        long dueChargeCount,
        long partiallyPaidChargeCount,
        long paidChargeCount,
        long overdueChargeCount,
        long paymentCount,
        long refundCount,
        long studentCount,
        long familyCount
) {}
