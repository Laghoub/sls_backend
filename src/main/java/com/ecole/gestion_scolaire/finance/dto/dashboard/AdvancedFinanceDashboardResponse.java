package com.ecole.gestion_scolaire.finance.dto.dashboard;

import java.util.List;

public record AdvancedFinanceDashboardResponse(
        FinanceDashboardFilterResponse filters,
        FinanceDashboardKpiResponse kpis,
        FinanceDashboardCashResponse cash,
        FinanceDashboardEmailResponse emails,
        List<FinanceDashboardBreakdownResponse> byFeeType,
        List<FinanceDashboardBreakdownResponse> byCycle,
        List<FinanceDashboardBreakdownResponse> byLevel,
        List<FinanceDashboardBreakdownResponse> byClassGroup,
        List<FinanceDashboardBreakdownResponse> byCampus,
        List<FinanceDashboardPaymentMethodResponse> byPaymentMethod,
        List<FinanceDashboardMonthlyTrendResponse> monthlyTrend,
        List<FinanceDashboardStatusResponse> chargeStatuses,
        List<FinanceDashboardDebtorResponse> topDebtors
) {}
