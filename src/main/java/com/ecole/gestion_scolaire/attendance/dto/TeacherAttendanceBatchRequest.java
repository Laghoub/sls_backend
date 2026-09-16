package com.ecole.gestion_scolaire.attendance.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record TeacherAttendanceBatchRequest(
        @NotEmpty List<@Valid TeacherAttendanceSaveRequest> attendances
) {}
