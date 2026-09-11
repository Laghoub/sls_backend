package com.ecole.gestion_scolaire.finance.dto.cash;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CashSessionOpenRequest(@NotNull Long cashRegisterId,
                                     @NotNull @DecimalMin("0.00") BigDecimal openingBalance) {
}
