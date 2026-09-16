package com.ecole.gestion_scolaire.timetable.dto;

import com.ecole.gestion_scolaire.timetable.enums.SchoolDay;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record ScheduleEntryRequest(
        @NotNull Long teachingAssignmentId,
        @NotNull SchoolDay dayOfWeek,
        @NotNull Long timeSlotId,
        Long roomId,
        @NotNull LocalDate validFrom,
        LocalDate validUntil
) {}
