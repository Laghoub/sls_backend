package com.ecole.gestion_scolaire.finance.dto.charge;

import java.math.BigDecimal;
import java.time.*;

public record StudentChargeResponse(Long id, Long studentEnrollmentId, Long registrationCaseId, Long feeTypeId,
                                    Long tariffId, String label, BigDecimal originalAmount, BigDecimal discountAmount,
                                    BigDecimal finalAmount, BigDecimal paidAmount, BigDecimal remainingAmount,
                                    LocalDate dueDate, LocalDate billingPeriodStart, LocalDate billingPeriodEnd,
                                    String status, OffsetDateTime createdAt) {
}
