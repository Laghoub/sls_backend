package com.ecole.gestion_scolaire.finance.dto.collection;

public record GuardianStudentResponse(
        Long studentId,
        String studentNumber,
        String lastName,
        String firstName,
        Long enrollmentId,
        Long schoolYearId,
        Long classGroupId,
        String classGroupName,
        String levelName,
        String campusName
) {}
