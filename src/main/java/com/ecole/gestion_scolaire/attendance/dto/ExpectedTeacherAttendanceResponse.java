package com.ecole.gestion_scolaire.attendance.dto;

import com.ecole.gestion_scolaire.attendance.enums.AttendanceValidationStatus;
import com.ecole.gestion_scolaire.attendance.enums.TeacherAttendanceStatus;
import java.time.LocalDate;
import java.time.LocalTime;

public record ExpectedTeacherAttendanceResponse(
        Long classSessionId,
        Long scheduleEntryId,
        LocalDate date,
        Long timeSlotId,
        String timeSlotCode,
        LocalTime startTime,
        LocalTime endTime,
        Long teacherId,
        String teacherName,
        Long classGroupId,
        String classGroupName,
        Long subjectId,
        String subjectName,
        Long roomId,
        String roomName,
        Long attendanceId,
        TeacherAttendanceStatus attendanceStatus,
        Integer lateMinutes,
        String reason,
        String notes,
        AttendanceValidationStatus validationStatus
) {}
