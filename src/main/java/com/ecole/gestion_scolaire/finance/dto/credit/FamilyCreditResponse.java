package com.ecole.gestion_scolaire.finance.dto.credit;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record FamilyCreditResponse(Long id, Long guardianId, Long sourcePaymentId, BigDecimal initialAmount,
                                   BigDecimal remainingAmount, String status, OffsetDateTime createdAt) {
}
