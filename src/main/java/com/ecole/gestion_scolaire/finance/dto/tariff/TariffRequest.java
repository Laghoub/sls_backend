package com.ecole.gestion_scolaire.finance.dto.tariff;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TariffRequest(@NotNull Long schoolYearId, @NotNull Long feeTypeId, Long levelId, Long classGroupId,
                            Long campusId, @NotNull @DecimalMin("0.00") BigDecimal amount,
                            @NotBlank String billingFrequency, @NotNull LocalDate validFrom, LocalDate validUntil,
                            boolean active) {
}
