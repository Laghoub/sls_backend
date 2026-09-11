package com.ecole.gestion_scolaire.school.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ClassGroupUpdateRequest(

        @NotNull
        Long schoolYearId,

        @NotNull
        Long levelId,

        @NotNull
        Long campusId,

        @NotBlank
        @Size(max = 50)
        String code,

        @NotBlank
        @Size(max = 100)
        String name,

        @PositiveOrZero
        Integer capacity,

        @NotBlank
        @Size(max = 30)
        String status
) {
}