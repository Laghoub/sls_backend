package com.ecole.gestion_scolaire.finance.controller;

import com.ecole.gestion_scolaire.common.dto.PageResponse;
import com.ecole.gestion_scolaire.finance.dto.credit.*;
import com.ecole.gestion_scolaire.finance.service.FamilyCreditService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/finance/family-credits")
public class FamilyCreditController {
    private final FamilyCreditService s;
    public FamilyCreditController(FamilyCreditService x) { s = x; }

    @GetMapping
    @PreAuthorize("hasAuthority('PAIEMENT_CONSULTER')")
    public PageResponse<FamilyCreditResponse> all(@RequestParam Long guardianId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) { return s.byGuardian(guardianId, page, size); }

    @GetMapping("/{id}/usages")
    @PreAuthorize("hasAuthority('PAIEMENT_CONSULTER')")
    public List<FamilyCreditUsageResponse> usages(@PathVariable Long id){ return s.usages(id); }

    @PostMapping("/{id}/apply")
    @PreAuthorize("hasAuthority('PAIEMENT_CREER')")
    public FamilyCreditUsageResponse apply(@PathVariable Long id, @Valid @RequestBody FamilyCreditApplyRequest request){ return s.apply(id, request); }
}
