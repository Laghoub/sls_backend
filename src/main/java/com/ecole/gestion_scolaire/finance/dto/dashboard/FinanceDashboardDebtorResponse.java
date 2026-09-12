package com.ecole.gestion_scolaire.finance.dto.dashboard;

import java.math.BigDecimal;

public record FinanceDashboardDebtorResponse(
        Long guardianId,
        String guardianName,
        String phone,
        long studentCount,
        BigDecimal remaining,
        BigDecimal overdue
) {}
