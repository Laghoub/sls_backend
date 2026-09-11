package com.ecole.gestion_scolaire.finance.dto.cash;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CashMovementRequest(@NotNull Long cashRegisterSessionId, @NotBlank String movementType,
                                  @NotBlank String direction, @NotNull @DecimalMin("0.01") BigDecimal amount,
                                  String reference, String description) {
}
