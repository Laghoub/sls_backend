package com.ecole.gestion_scolaire.hr.dto;

public record EmployeeAccountActivationResponse(
        Long employeeId,
        Long userAccountId,
        String username,
        String temporaryPassword,
        String roleCode,
        boolean mustChangePassword
) {}
