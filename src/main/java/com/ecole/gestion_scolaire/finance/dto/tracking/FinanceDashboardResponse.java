package com.ecole.gestion_scolaire.finance.dto.tracking;

import java.math.BigDecimal;

public record FinanceDashboardResponse(
        Long schoolYearId,
        BigDecimal totalCharged,
        BigDecimal totalCollected,
        BigDecimal totalRemaining,
        BigDecimal overdueAmount,
        long dueChargeCount,
        long partiallyPaidChargeCount,
        long paidChargeCount,
        long overdueChargeCount,
        BigDecimal availableFamilyCredit
) {
}
