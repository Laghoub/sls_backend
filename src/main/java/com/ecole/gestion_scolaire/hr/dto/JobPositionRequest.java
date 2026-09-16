package com.ecole.gestion_scolaire.hr.dto;

import jakarta.validation.constraints.NotBlank;

public record JobPositionRequest(
        @NotBlank String code,
        @NotBlank String name,
        String category,
        Boolean active,
        Integer displayOrder
) {}
