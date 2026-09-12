package com.ecole.gestion_scolaire.finance.dto.billing;

import java.math.BigDecimal;
import java.util.List;

public record BillingCreationResponse(
        Long tariffId, int createdCount, int skippedCount, BigDecimal createdAmount, List<Long> chargeIds
) {}
