package com.ecole.gestion_scolaire.school.dto;

public record LevelResponse(

        Long id,

        Long cycleId,

        String cycleCode,

        String cycleName,

        String code,

        String name,

        Integer displayOrder,

        boolean active
) {
}