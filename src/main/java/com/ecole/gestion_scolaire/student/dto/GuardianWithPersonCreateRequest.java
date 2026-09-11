package com.ecole.gestion_scolaire.student.dto;

import com.ecole.gestion_scolaire.identity.dto.PersonCreateRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record GuardianWithPersonCreateRequest(

        @NotNull
        @Valid
        PersonCreateRequest person,

        @NotBlank
        @Size(max = 30)
        String status
) {
}