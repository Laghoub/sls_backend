package com.ecole.gestion_scolaire.school.dto;

import com.ecole.gestion_scolaire.school.entity.enums.SchoolYearStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record SchoolYearUpdateRequest(

        @NotBlank
        @Size(max = 20)
        String code,

        @NotBlank
        @Size(max = 50)
        String label,

        @NotNull
        LocalDate startDate,

        @NotNull
        LocalDate endDate,

        @NotNull
        SchoolYearStatus status
) {
}