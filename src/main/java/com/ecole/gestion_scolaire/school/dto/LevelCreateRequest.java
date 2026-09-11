package com.ecole.gestion_scolaire.school.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LevelCreateRequest(

        @NotNull
        Long cycleId,

        @NotBlank
        @Size(max = 30)
        String code,

        @NotBlank
        @Size(max = 100)
        String name,

        @NotNull
        Integer displayOrder,

        boolean active
) {
}