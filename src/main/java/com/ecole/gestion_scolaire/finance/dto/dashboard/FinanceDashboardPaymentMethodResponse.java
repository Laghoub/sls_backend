package com.ecole.gestion_scolaire.finance.dto.dashboard;

import java.math.BigDecimal;

public record FinanceDashboardPaymentMethodResponse(
        Long id,
        String code,
        String label,
        BigDecimal amount,
        long paymentCount,
        BigDecimal sharePercent
) {}
