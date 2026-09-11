package com.ecole.gestion_scolaire.finance.dto.credit;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record FamilyCreditApplyRequest(
        @NotNull Long studentChargeId,
        @NotNull @DecimalMin("0.01") BigDecimal amount
) {
}
