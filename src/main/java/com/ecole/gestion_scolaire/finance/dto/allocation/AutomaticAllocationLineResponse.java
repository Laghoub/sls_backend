package com.ecole.gestion_scolaire.finance.dto.allocation;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AutomaticAllocationLineResponse(
        Long studentChargeId,
        Long studentId,
        String studentNumber,
        String studentLastName,
        String studentFirstName,
        String label,
        LocalDate dueDate,
        LocalDate billingPeriodStart,
        LocalDate billingPeriodEnd,
        BigDecimal remainingBefore,
        BigDecimal allocatedAmount,
        BigDecimal remainingAfter,
        boolean fullyPaid
) {}
