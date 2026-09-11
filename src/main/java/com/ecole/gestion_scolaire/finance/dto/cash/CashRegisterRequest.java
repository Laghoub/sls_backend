package com.ecole.gestion_scolaire.finance.dto.cash;

import jakarta.validation.constraints.*;

public record CashRegisterRequest(@NotBlank String code, @NotBlank String name, Long campusId, boolean active) {
}
