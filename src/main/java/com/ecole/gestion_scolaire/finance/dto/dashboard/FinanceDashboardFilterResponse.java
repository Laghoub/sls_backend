package com.ecole.gestion_scolaire.finance.dto.dashboard;

import java.time.LocalDate;

public record FinanceDashboardFilterResponse(
        Long schoolYearId,
        Long cycleId,
        Long levelId,
        Long classGroupId,
        Long campusId,
        Long feeTypeId,
        Long paymentMethodId,
        LocalDate from,
        LocalDate to
) {}
