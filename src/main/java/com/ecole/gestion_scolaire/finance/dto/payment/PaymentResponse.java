package com.ecole.gestion_scolaire.finance.dto.payment;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PaymentResponse(Long id, String paymentNumber, Long guardianId, OffsetDateTime paymentDate,
                              Long paymentMethodId, BigDecimal totalAmount, String status, Long cashierId,
                              Long cashRegisterSessionId, String externalReference, String notes,
                              OffsetDateTime createdAt) {
}
