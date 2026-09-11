package com.ecole.gestion_scolaire.school.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record SchoolCalendarExceptionUpdateRequest(

        @NotNull
        Long schoolYearId,

        Long campusId,

        Long cycleId,

        @NotNull
        LocalDate exceptionDate,

        @NotBlank
        @Size(max = 40)
        String exceptionType,

        @NotBlank
        @Size(max = 200)
        String label,

        String description
) {
}