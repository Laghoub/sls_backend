package com.ecole.gestion_scolaire.finance.controller;

import com.ecole.gestion_scolaire.finance.dto.billing.*;
import com.ecole.gestion_scolaire.finance.service.TariffBillingService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/finance/billing")
public class TariffBillingController {
    private final TariffBillingService service;

    public TariffBillingController(TariffBillingService service) {
        this.service = service;
    }

    @PostMapping("/preview")
    @PreAuthorize("hasAuthority('CREANCE_CONSULTER')")
    public BillingPreviewResponse preview(@Valid @RequestBody TariffBillingRequest request) {
        return service.preview(request);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CREANCE_GERER')")
    public BillingCreationResponse create(@Valid @RequestBody TariffBillingRequest request) {
        return service.create(request);
    }
}
