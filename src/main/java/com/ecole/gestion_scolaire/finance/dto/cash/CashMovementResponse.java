package com.ecole.gestion_scolaire.finance.dto.cash;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CashMovementResponse(Long id, Long cashRegisterSessionId, String movementType, String direction,
                                   BigDecimal amount, Long paymentId, Long refundId, Long salaryPaymentId, Long providerPaymentId, String reference,
                                   String description, Long createdBy, OffsetDateTime createdAt) {
}
