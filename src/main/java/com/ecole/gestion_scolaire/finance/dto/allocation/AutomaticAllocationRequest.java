package com.ecole.gestion_scolaire.finance.dto.allocation;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AutomaticAllocationRequest(
        @NotNull Long guardianId,
        Long schoolYearId,
        @NotNull @DecimalMin("0.01") BigDecimal amount
) {}
