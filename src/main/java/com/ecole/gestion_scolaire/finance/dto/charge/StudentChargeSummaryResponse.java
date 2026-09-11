package com.ecole.gestion_scolaire.finance.dto.charge;

import java.math.BigDecimal;

public record StudentChargeSummaryResponse(
        long totalCharges,
        BigDecimal totalDue,
        BigDecimal totalPaid,
        BigDecimal totalRemaining
) {}
