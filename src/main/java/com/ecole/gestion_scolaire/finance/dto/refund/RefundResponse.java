package com.ecole.gestion_scolaire.finance.dto.refund;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record RefundResponse(Long id, Long paymentId, OffsetDateTime refundDate, BigDecimal amount,
                             Long paymentMethodId, String reason, String status, Long createdBy, Long validatedBy,
                             OffsetDateTime validatedAt, OffsetDateTime createdAt,
                             BigDecimal creditReversedAmount, BigDecimal allocationReversedAmount) {
}
