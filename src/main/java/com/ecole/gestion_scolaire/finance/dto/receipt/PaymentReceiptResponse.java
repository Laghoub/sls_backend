package com.ecole.gestion_scolaire.finance.dto.receipt;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record PaymentReceiptResponse(
        Long paymentId,
        String paymentNumber,
        OffsetDateTime paymentDate,
        BigDecimal totalAmount,
        String status,
        Long guardianId,
        String guardianLastName,
        String guardianFirstName,
        String guardianPhone,
        String guardianEmail,
        Long paymentMethodId,
        String paymentMethodCode,
        String paymentMethodName,
        String externalReference,
        List<PaymentReceiptAllocationResponse> allocations
) {
}
