package com.ecole.gestion_scolaire.finance.dto.tracking;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OverdueChargeResponse(
        Long chargeId,
        Long studentId,
        String studentNumber,
        String studentLastName,
        String studentFirstName,
        Long guardianId,
        String guardianLastName,
        String guardianFirstName,
        String guardianPhone,
        String label,
        LocalDate dueDate,
        BigDecimal finalAmount,
        BigDecimal paidAmount,
        BigDecimal remainingAmount,
        long daysLate,
        String status
) {
}
