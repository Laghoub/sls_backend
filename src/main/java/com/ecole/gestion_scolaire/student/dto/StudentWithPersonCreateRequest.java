package com.ecole.gestion_scolaire.student.dto;

import com.ecole.gestion_scolaire.identity.dto.PersonCreateRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record StudentWithPersonCreateRequest(

        @NotNull
        @Valid
        PersonCreateRequest person,

        LocalDate initialAdmissionDate,

        @NotBlank
        @Size(max = 30)
        String status
) {
}