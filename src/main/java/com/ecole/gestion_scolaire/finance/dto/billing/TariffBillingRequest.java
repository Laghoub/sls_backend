package com.ecole.gestion_scolaire.finance.dto.billing;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record TariffBillingRequest(
        @NotNull Long tariffId,
        @NotNull BillingTargetType targetType,
        Long targetId,
        List<Long> studentIds,
        LocalDate dueDate
) {}
