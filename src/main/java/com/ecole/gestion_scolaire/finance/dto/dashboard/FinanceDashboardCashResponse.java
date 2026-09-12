package com.ecole.gestion_scolaire.finance.dto.dashboard;

import java.math.BigDecimal;

public record FinanceDashboardCashResponse(
        BigDecimal openingFunds,
        BigDecimal cashInflows,
        BigDecimal cashOutflows,
        BigDecimal netCashMovement,
        BigDecimal expectedClosingTotal,
        BigDecimal actualClosingTotal,
        BigDecimal totalDifference,
        long openSessionCount,
        long closedSessionCount
) {}
