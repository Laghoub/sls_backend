package com.ecole.gestion_scolaire.finance.dto.installment;

import java.util.List;

public record InstallmentGenerationResponse(
        Long studentEnrollmentId,
        int createdCount,
        int skippedCount,
        List<Long> createdChargeIds
) {
}
