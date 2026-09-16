package com.ecole.gestion_scolaire.hr.dto;

import com.ecole.gestion_scolaire.hr.enums.EmploymentStatus;
import java.time.LocalDate;

public record EmployeeResponse(
        Long id,
        Long personId,
        String employeeNumber,
        String firstName,
        String lastName,
        String email,
        String phone,
        Long positionId,
        String positionCode,
        String positionTitle,
        Long campusId,
        String campusName,
        LocalDate hireDate,
        LocalDate endDate,
        EmploymentStatus status,
        boolean hasUserAccount
) {}
