package com.ecole.gestion_scolaire.finance.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FinanceDashboardMonthlyTrendResponse(
        LocalDate month,
        BigDecimal charged,
        BigDecimal collected,
        BigDecimal refunds,
        BigDecimal netCollected
) {}
