package com.ecole.gestion_scolaire.attendance.dto;

import com.ecole.gestion_scolaire.attendance.enums.AttendanceValidationStatus;
import com.ecole.gestion_scolaire.attendance.enums.TeacherAttendanceStatus;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

public record TeacherAttendanceResponse(
        Long id,
        Long classSessionId,
        LocalDate date,
        Long teacherId,
        String teacherName,
        Long classGroupId,
        String classGroupName,
        Long subjectId,
        String subjectName,
        Long timeSlotId,
        String timeSlotCode,
        LocalTime startTime,
        LocalTime endTime,
        TeacherAttendanceStatus attendanceStatus,
        Integer lateMinutes,
        String reason,
        String notes,
        AttendanceValidationStatus validationStatus,
        String recordedBy,
        OffsetDateTime recordedAt,
        String validatedBy,
        OffsetDateTime validatedAt
) {}
