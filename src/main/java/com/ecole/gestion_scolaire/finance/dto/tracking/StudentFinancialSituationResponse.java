package com.ecole.gestion_scolaire.finance.dto.tracking;

import java.math.BigDecimal;
import java.util.List;

public record StudentFinancialSituationResponse(
        Long studentId,
        String studentNumber,
        String lastName,
        String firstName,
        Long schoolYearId,
        Long studentEnrollmentId,
        Long classGroupId,
        BigDecimal totalCharged,
        BigDecimal totalPaid,
        BigDecimal totalRemaining,
        BigDecimal overdueAmount,
        List<FinancialChargeLineResponse> charges
) {
}
