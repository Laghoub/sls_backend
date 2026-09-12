package com.ecole.gestion_scolaire.finance.dto.dashboard;

public record FinanceDashboardEmailResponse(
        long sent,
        long pending,
        long failedOrRetrying
) {}
