package com.ecole.gestion_scolaire.school.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

public record TimeSlotUpdateRequest(

        @NotBlank
        @Size(max = 30)
        String code,

        @NotNull
        LocalTime startTime,

        @NotNull
        LocalTime endTime,

        @NotNull
        Integer displayOrder,

        boolean active
) {
}