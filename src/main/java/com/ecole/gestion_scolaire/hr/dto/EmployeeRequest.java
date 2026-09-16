package com.ecole.gestion_scolaire.hr.dto;

import com.ecole.gestion_scolaire.hr.enums.EmploymentStatus;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record EmployeeRequest(
        @NotNull Long personId,
        String socialSecurityNumber,
        Long positionId,
        String positionTitle,
        Long campusId,
        LocalDate hireDate,
        LocalDate endDate,
        EmploymentStatus employmentStatus,
        String maritalStatus
) {}
