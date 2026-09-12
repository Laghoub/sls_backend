package com.ecole.gestion_scolaire.finance.dto.discount;
import jakarta.validation.constraints.*; import java.math.BigDecimal; import java.time.LocalDate;
public record StudentDiscountRequest(@NotNull Long studentEnrollmentId, Long feeTypeId, @NotBlank String discountType, @NotNull @DecimalMin("0.00") BigDecimal value, @NotNull LocalDate startDate, LocalDate endDate, String reason){}
