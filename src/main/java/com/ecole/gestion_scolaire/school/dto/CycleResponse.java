package com.ecole.gestion_scolaire.school.dto;

public record CycleResponse(

        Long id,

        String code,

        String name,

        Integer displayOrder,

        boolean active
) {
}