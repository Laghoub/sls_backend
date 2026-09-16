package com.ecole.gestion_scolaire.attendance.dto;

import com.ecole.gestion_scolaire.attendance.enums.TeacherAttendanceStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record TeacherAttendanceSaveRequest(
        @NotNull Long scheduleEntryId,
        @NotNull LocalDate date,
        @NotNull TeacherAttendanceStatus status,
        @Min(0) Integer lateMinutes,
        String reason,
        String notes
) {}
