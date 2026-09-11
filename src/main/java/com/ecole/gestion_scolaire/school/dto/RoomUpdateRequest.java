package com.ecole.gestion_scolaire.school.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record RoomUpdateRequest(

        @NotNull
        Long campusId,

        @NotBlank
        @Size(max = 30)
        String code,

        @NotBlank
        @Size(max = 100)
        String name,

        @PositiveOrZero
        Integer capacity,

        @Size(max = 30)
        String roomType,

        boolean active
) {
}