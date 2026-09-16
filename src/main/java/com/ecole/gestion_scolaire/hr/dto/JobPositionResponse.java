package com.ecole.gestion_scolaire.hr.dto;

public record JobPositionResponse(
        Long id,
        String code,
        String name,
        String category,
        boolean active,
        Integer displayOrder
) {}
