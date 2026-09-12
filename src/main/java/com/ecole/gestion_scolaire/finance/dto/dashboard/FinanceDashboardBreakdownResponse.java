package com.ecole.gestion_scolaire.finance.dto.dashboard;

import java.math.BigDecimal;

public record FinanceDashboardBreakdownResponse(
        Long id,
        String code,
        String label,
        BigDecimal charged,
        BigDecimal collected,
        BigDecimal remaining,
        BigDecimal overdue,
        long chargeCount
) {}
