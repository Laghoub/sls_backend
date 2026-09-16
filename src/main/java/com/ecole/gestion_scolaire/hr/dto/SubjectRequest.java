package com.ecole.gestion_scolaire.hr.dto;

import jakarta.validation.constraints.NotBlank;

public record SubjectRequest(
        @NotBlank String code,
        @NotBlank String name,
        Boolean active,
        Integer displayOrder
) {}
