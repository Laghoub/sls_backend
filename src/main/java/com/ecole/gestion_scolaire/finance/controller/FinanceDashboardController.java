package com.ecole.gestion_scolaire.finance.controller;

import com.ecole.gestion_scolaire.finance.dto.dashboard.AdvancedFinanceDashboardResponse;
import com.ecole.gestion_scolaire.finance.service.FinanceDashboardAnalyticsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;

@RestController
@RequestMapping("/api/finance/dashboard")
public class FinanceDashboardController {

    private final FinanceDashboardAnalyticsService dashboard;
    private final JdbcTemplate db;

    public FinanceDashboardController(FinanceDashboardAnalyticsService dashboard, JdbcTemplate db) {
        this.dashboard = dashboard;
        this.db = db;
    }

    @GetMapping("/analytics")
    @PreAuthorize("hasAuthority('PAIEMENT_CONSULTER') or hasAuthority('CAISSE_CONSULTER')")
    public AdvancedFinanceDashboardResponse analytics(
            @RequestParam(required = false) Long schoolYearId,
            @RequestParam(required = false) Long cycleId,
            @RequestParam(required = false) Long levelId,
            @RequestParam(required = false) Long classGroupId,
            @RequestParam(required = false) Long campusId,
            @RequestParam(required = false) Long feeTypeId,
            @RequestParam(required = false) Long paymentMethodId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return dashboard.dashboard(schoolYearId, cycleId, levelId, classGroupId, campusId, feeTypeId, paymentMethodId, from, to);
    }
    /** Cash movements are the sole source for paid totals: salary/expense payments are not added again. */
    @GetMapping("/expenses-summary")
    @PreAuthorize("hasAuthority('PAIEMENT_CONSULTER') or hasAuthority('CAISSE_CONSULTER') or hasAuthority('DEPENSES_RAPPORTS')")
    public Map<String, Object> expensesSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        String cashSql = "SELECT " +
                "COALESCE(SUM(amount) FILTER (WHERE direction='IN'),0) cashInflows, " +
                "COALESCE(SUM(amount) FILTER (WHERE direction='OUT'),0) cashOutflows, " +
                "COALESCE(SUM(amount) FILTER (WHERE direction='OUT' AND salary_payment_id IS NOT NULL),0) teacherPayroll, " +
                "COALESCE(SUM(amount) FILTER (WHERE direction='OUT' AND staff_pay_payment_id IS NOT NULL),0) staffPayroll, " +
                "COALESCE(SUM(amount) FILTER (WHERE direction='OUT' AND expense_payment_id IS NOT NULL),0) otherExpenses " +
                "FROM cash_movement WHERE (?::date IS NULL OR created_at >= ?::date) " +
                "AND (?::date IS NULL OR created_at < (?::date + INTERVAL '1 day'))";
        Map<String,Object> cash = db.queryForMap(cashSql, from,from,to,to);
        Map<String,Object> obligations = db.queryForMap(
                "SELECT COALESCE(SUM(GREATEST(amount-COALESCE(paid_amount,0),0)),0) unpaidExpenses " +
                "FROM expense WHERE status IN ('APPROVED','PARTIALLY_PAID') " +
                "AND (?::date IS NULL OR expense_date >= ?::date) " +
                "AND (?::date IS NULL OR expense_date <= ?::date)", from,from,to,to);
        java.util.Map<String,Object> result = new java.util.LinkedHashMap<>(cash);
        result.putAll(obligations);
        result.put("scope", "CASH_REGISTER_MOVEMENTS_ONLY");
        return result;
    }

}
