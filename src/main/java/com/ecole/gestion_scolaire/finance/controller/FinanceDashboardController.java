package com.ecole.gestion_scolaire.finance.controller;

import com.ecole.gestion_scolaire.finance.dto.dashboard.AdvancedFinanceDashboardResponse;
import com.ecole.gestion_scolaire.finance.service.FinanceDashboardAnalyticsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/finance/dashboard")
public class FinanceDashboardController {

    private final FinanceDashboardAnalyticsService dashboard;

    public FinanceDashboardController(FinanceDashboardAnalyticsService dashboard) {
        this.dashboard = dashboard;
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
}
