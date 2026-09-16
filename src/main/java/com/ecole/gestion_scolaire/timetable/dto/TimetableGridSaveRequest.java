package com.ecole.gestion_scolaire.timetable.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record TimetableGridSaveRequest(
        @NotNull Long schoolYearId,
        @NotNull Long classGroupId,
        @NotEmpty List<@Valid ScheduleEntryRequest> entries
) {}
