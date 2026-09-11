package com.ecole.gestion_scolaire.finance.dto.tracking;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FinancialChargeLineResponse(
        Long chargeId,
        Long feeTypeId,
        String feeTypeCode,
        String feeTypeName,
        String label,
        BigDecimal finalAmount,
        BigDecimal paidAmount,
        BigDecimal remainingAmount,
        LocalDate dueDate,
        LocalDate billingPeriodStart,
        LocalDate billingPeriodEnd,
        String status,
        boolean overdue
) {
}
