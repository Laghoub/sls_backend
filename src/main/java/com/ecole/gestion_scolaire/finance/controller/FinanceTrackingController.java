package com.ecole.gestion_scolaire.finance.controller;

import com.ecole.gestion_scolaire.common.dto.PageResponse;
import com.ecole.gestion_scolaire.finance.dto.installment.*;
import com.ecole.gestion_scolaire.finance.dto.tracking.*;
import com.ecole.gestion_scolaire.finance.service.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/finance/tracking")
public class FinanceTrackingController {
    private final FinancialTrackingService tracking; private final BillingScheduleService schedules;
    public FinanceTrackingController(FinancialTrackingService t, BillingScheduleService s){tracking=t;schedules=s;}

    @GetMapping("/students/{studentId}")
    @PreAuthorize("hasAuthority('CREANCE_CONSULTER')")
    public StudentFinancialSituationResponse student(@PathVariable Long studentId,@RequestParam Long schoolYearId){return tracking.student(studentId,schoolYearId);}

    @GetMapping("/families/{guardianId}")
    @PreAuthorize("hasAuthority('CREANCE_CONSULTER')")
    public FamilyFinancialSituationResponse family(@PathVariable Long guardianId,@RequestParam Long schoolYearId){return tracking.family(guardianId,schoolYearId);}

    @GetMapping("/overdue")
    @PreAuthorize("hasAuthority('CREANCE_CONSULTER')")
    public PageResponse<OverdueChargeResponse> overdue(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return tracking.overdue(page,size);}

    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('PAIEMENT_CONSULTER')")
    public FinanceDashboardResponse dashboard(@RequestParam Long schoolYearId){return tracking.dashboard(schoolYearId);}

    @PostMapping("/enrollments/{enrollmentId}/generate-installments")
    @PreAuthorize("hasAuthority('CREANCE_GERER')")
    public InstallmentGenerationResponse generate(@PathVariable Long enrollmentId,@RequestBody(required=false) InstallmentGenerationRequest body){return schedules.generate(enrollmentId,body==null?new InstallmentGenerationRequest(null,null,null):body);}
}
