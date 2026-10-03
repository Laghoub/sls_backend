package com.ecole.gestion_scolaire.finance.dto.global;

import java.math.BigDecimal;
import java.util.List;

public record GlobalFinancialSituationResponse(
        Long schoolYearId,
        Integer cutoffMonth,
        GlobalSummary summary,
        List<FamilyRow> families
) {
    public record GlobalSummary(
            BigDecimal expectedAnnual,
            BigDecimal expectedToDate,
            BigDecimal collected,
            BigDecimal remainingToDate,
            BigDecimal overdue,
            BigDecimal availableCredits,
            BigDecimal collectionRate,
            long familiesCount,
            long familiesUpToDate,
            long familiesWithDebt
    ) {}

    public record FamilyRow(
            Long guardianId,
            String guardianLastName,
            String guardianFirstName,
            String phone,
            String email,
            List<ChildInfo> children,
            PaymentCell registration,
            List<PaymentCell> months,
            BigDecimal expectedToDate,
            BigDecimal paidToDate,
            BigDecimal remainingToDate,
            String globalStatus
    ) {}

    public record ChildInfo(
            Long studentId,
            String studentNumber,
            String lastName,
            String firstName,
            Long classGroupId,
            String className
    ) {}

    public record PaymentCell(
            String key,
            String label,
            BigDecimal expected,
            BigDecimal paid,
            BigDecimal remaining,
            String status,
            boolean overdue
    ) {}
}
