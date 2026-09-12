package com.ecole.gestion_scolaire.finance.dto.collection;

import java.math.BigDecimal;
import java.time.LocalDate;

public record StudentOpenChargeResponse(
        Long chargeId,
        Long studentId,
        Long studentEnrollmentId,
        Long registrationCaseId,
        String studentNumber,
        String studentLastName,
        String studentFirstName,
        String label,
        Long feeTypeId,
        BigDecimal finalAmount,
        BigDecimal paidAmount,
        BigDecimal remainingAmount,
        LocalDate dueDate,
        LocalDate billingPeriodStart,
        LocalDate billingPeriodEnd,
        String status
) {}
