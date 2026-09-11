package com.ecole.gestion_scolaire.school.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CampusUpdateRequest(

        @NotBlank
        @Size(max = 30)
        String code,

        @NotBlank
        @Size(max = 150)
        String name,

        @Size(max = 300)
        String address,

        @Size(max = 30)
        String phone,

        boolean active
) {
}