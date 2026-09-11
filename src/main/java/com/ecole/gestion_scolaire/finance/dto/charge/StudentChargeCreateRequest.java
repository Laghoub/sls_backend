package com.ecole.gestion_scolaire.finance.dto.charge;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record StudentChargeCreateRequest(Long studentEnrollmentId, Long registrationCaseId, @NotNull Long feeTypeId,
                                         Long tariffId, @NotBlank String label,
                                         @NotNull @DecimalMin("0.00") BigDecimal originalAmount,
                                         @NotNull @DecimalMin("0.00") BigDecimal discountAmount, LocalDate dueDate,
                                         LocalDate billingPeriodStart, LocalDate billingPeriodEnd) {
}
