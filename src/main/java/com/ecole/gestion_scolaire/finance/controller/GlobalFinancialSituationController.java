package com.ecole.gestion_scolaire.finance.controller;

import com.ecole.gestion_scolaire.finance.dto.global.GlobalFinancialSituationResponse;
import com.ecole.gestion_scolaire.finance.service.GlobalFinancialSituationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/finance/global-situation")
public class GlobalFinancialSituationController {
    private final GlobalFinancialSituationService service;
    public GlobalFinancialSituationController(GlobalFinancialSituationService service){this.service=service;}

    @GetMapping
    @PreAuthorize("hasAuthority('PAIEMENT_CONSULTER') or hasAuthority('CREANCE_CONSULTER')")
    public GlobalFinancialSituationResponse get(
            @RequestParam Long schoolYearId,
            @RequestParam(required=false) Long classGroupId,
            @RequestParam(required=false) String parent,
            @RequestParam(required=false) Integer cutoffMonth,
            @RequestParam(required=false) String status) {
        return service.get(schoolYearId,classGroupId,parent,cutoffMonth,status);
    }
}
