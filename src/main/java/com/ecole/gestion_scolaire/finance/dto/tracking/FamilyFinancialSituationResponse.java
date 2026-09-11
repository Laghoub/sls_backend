package com.ecole.gestion_scolaire.finance.dto.tracking;

import java.math.BigDecimal;
import java.util.List;

public record FamilyFinancialSituationResponse(
        Long guardianId,
        String lastName,
        String firstName,
        String phone,
        String email,
        Long schoolYearId,
        BigDecimal totalCharged,
        BigDecimal totalPaid,
        BigDecimal totalRemaining,
        BigDecimal overdueAmount,
        BigDecimal availableCredit,
        List<StudentFinancialSituationResponse> students
) {
}
