package com.ecole.gestion_scolaire.timetable.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record ScheduleEntryResponse(
        Long id,
        Long schoolYearId,
        String schoolYear,
        Long classGroupId,
        String classGroupName,
        Long teachingAssignmentId,
        Long teacherId,
        String teacherName,
        Long subjectId,
        String subjectName,
        String dayOfWeek,
        Long timeSlotId,
        String timeSlotCode,
        LocalTime startTime,
        LocalTime endTime,
        Long roomId,
        String roomName,
        LocalDate validFrom,
        LocalDate validUntil,
        String status
) {}
