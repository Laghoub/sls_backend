package com.ecole.gestion_scolaire.school.dto;

import java.time.LocalTime;

public record TimeSlotResponse(

        Long id,

        String code,

        LocalTime startTime,

        LocalTime endTime,

        Integer displayOrder,

        boolean active
) {
}