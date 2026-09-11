package com.ecole.gestion_scolaire.school.dto;

import java.time.LocalDate;

public record SchoolCalendarExceptionResponse(

        Long id,

        Long schoolYearId,
        String schoolYearCode,
        String schoolYearLabel,

        Long campusId,
        String campusCode,
        String campusName,

        Long cycleId,
        String cycleCode,
        String cycleName,

        LocalDate exceptionDate,

        String exceptionType,

        String label,

        String description
) {
}