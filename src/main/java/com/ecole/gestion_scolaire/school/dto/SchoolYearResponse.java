package com.ecole.gestion_scolaire.school.dto;

import com.ecole.gestion_scolaire.school.entity.enums.SchoolYearStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record SchoolYearResponse(

        Long id,

        String code,

        String label,

        LocalDate startDate,

        LocalDate endDate,

        SchoolYearStatus status,

        boolean currentYear,

        OffsetDateTime createdAt,

        OffsetDateTime updatedAt
) {
}