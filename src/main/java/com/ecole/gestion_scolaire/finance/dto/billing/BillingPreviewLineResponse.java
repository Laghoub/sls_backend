package com.ecole.gestion_scolaire.finance.dto.billing;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BillingPreviewLineResponse(
        Long enrollmentId, Long studentId, String studentNumber, String studentLastName, String studentFirstName,
        Long classGroupId, String classGroupName, Long levelId, String levelName, Long cycleId, String cycleName,
        LocalDate periodStart, LocalDate periodEnd, LocalDate dueDate, BigDecimal amount, boolean alreadyExists
) {}
