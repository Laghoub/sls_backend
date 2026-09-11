package com.ecole.gestion_scolaire.finance.dto.payment;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record PaymentAllocationRequest(@NotNull Long studentChargeId,
                                       @NotNull @DecimalMin(value = "0.01") BigDecimal amount) {
}
