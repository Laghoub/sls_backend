package com.ecole.gestion_scolaire.hr.dto;

public record SubjectResponse(
        Long id,
        String code,
        String name,
        boolean active,
        Integer displayOrder
) {}
