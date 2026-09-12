package com.ecole.gestion_scolaire.finance.dto.dashboard;

import java.math.BigDecimal;

public record FinanceDashboardStatusResponse(
        String status,
        long count,
        BigDecimal amount,
        BigDecimal remaining
) {}
