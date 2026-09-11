package com.ecole.gestion_scolaire.finance.dto.credit;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record FamilyCreditUsageResponse(
        Long id,
        Long familyCreditId,
        Long guardianId,
        Long studentChargeId,
        Long paymentAllocationId,
        BigDecimal amount,
        OffsetDateTime createdAt
) {
}
