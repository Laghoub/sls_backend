package com.ecole.gestion_scolaire.identity.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record PersonResponse(

        Long id,

        String lastName,

        String firstName,

        LocalDate birthDate,

        String birthPlace,

        String nationality,

        String sex,

        String address,

        String phone,

        String secondaryPhone,

        String email,

        String photoReference,

        OffsetDateTime createdAt,

        OffsetDateTime updatedAt
) {
}