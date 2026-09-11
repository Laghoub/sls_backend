package com.ecole.gestion_scolaire.school.dto;

import java.time.OffsetDateTime;

public record ClassGroupResponse(

        Long id,

        Long schoolYearId,
        String schoolYearCode,
        String schoolYearLabel,

        Long levelId,
        String levelCode,
        String levelName,

        Long cycleId,
        String cycleCode,
        String cycleName,

        Long campusId,
        String campusCode,
        String campusName,

        String code,
        String name,
        Integer capacity,
        String status,

        OffsetDateTime createdAt
) {
}