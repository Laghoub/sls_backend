package com.ecole.gestion_scolaire.timetable.dto;

public record EligibleAssignmentResponse(
        Long teachingAssignmentId,
        Long teacherId,
        String teacherName,
        Long subjectId,
        String subjectName,
        Long classGroupId,
        String classGroupName
) {}
