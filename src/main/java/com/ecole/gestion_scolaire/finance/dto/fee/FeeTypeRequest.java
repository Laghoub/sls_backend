package com.ecole.gestion_scolaire.finance.dto.fee;

import jakarta.validation.constraints.*;

public record FeeTypeRequest(@NotBlank String code, @NotBlank String name, @NotBlank String category, boolean active,
                             Integer displayOrder) {
}
