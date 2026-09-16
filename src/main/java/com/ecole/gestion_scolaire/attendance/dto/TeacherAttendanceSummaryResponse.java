package com.ecole.gestion_scolaire.attendance.dto;

public record TeacherAttendanceSummaryResponse(
        long total,
        long present,
        long absent,
        long late,
        long pending,
        long validated
) {}
