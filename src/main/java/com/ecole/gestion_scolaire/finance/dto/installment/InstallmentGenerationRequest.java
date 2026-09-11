package com.ecole.gestion_scolaire.finance.dto.installment;

import java.time.LocalDate;

public record InstallmentGenerationRequest(
        Long feeTypeId,
        LocalDate fromDate,
        LocalDate toDate
) {
}
