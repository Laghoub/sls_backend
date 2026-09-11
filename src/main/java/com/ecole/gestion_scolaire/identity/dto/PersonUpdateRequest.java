package com.ecole.gestion_scolaire.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PersonUpdateRequest(

        @NotBlank
        @Size(max = 100)
        String lastName,

        @NotBlank
        @Size(max = 100)
        String firstName,

        LocalDate birthDate,

        @Size(max = 150)
        String birthPlace,

        @Size(max = 80)
        String nationality,

        @Size(max = 20)
        String sex,

        @Size(max = 300)
        String address,

        @Size(max = 30)
        String phone,

        @Size(max = 30)
        String secondaryPhone,

        @Email
        @Size(max = 150)
        String email,

        @Size(max = 500)
        String photoReference
) {
}