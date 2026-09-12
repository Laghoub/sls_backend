package com.ecole.gestion_scolaire.finance.dto.receipt;

import java.math.BigDecimal;

public record PaymentReceiptAllocationResponse(
        Long chargeId,
        Long studentId,
        String studentNumber,
        String studentLastName,
        String studentFirstName,
        String chargeLabel,
        BigDecimal amount,
        BigDecimal refundedAmount,
        BigDecimal netAmount
) {
}
