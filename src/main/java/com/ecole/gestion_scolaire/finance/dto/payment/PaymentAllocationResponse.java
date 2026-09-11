package com.ecole.gestion_scolaire.finance.dto.payment;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PaymentAllocationResponse(Long id, Long paymentId, Long studentChargeId, BigDecimal amount,
                                        OffsetDateTime createdAt) {
}
