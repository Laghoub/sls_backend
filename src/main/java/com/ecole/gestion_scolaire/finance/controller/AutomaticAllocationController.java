package com.ecole.gestion_scolaire.finance.controller;

import com.ecole.gestion_scolaire.finance.dto.allocation.AutomaticAllocationRequest;
import com.ecole.gestion_scolaire.finance.dto.allocation.AutomaticAllocationResponse;
import com.ecole.gestion_scolaire.finance.service.AutomaticAllocationService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/finance/collection")
public class AutomaticAllocationController {

    private final AutomaticAllocationService service;

    public AutomaticAllocationController(AutomaticAllocationService service) {
        this.service = service;
    }

    @PostMapping("/automatic-allocation/preview")
    @PreAuthorize("hasAuthority('PAIEMENT_CREER')")
    public AutomaticAllocationResponse preview(@Valid @RequestBody AutomaticAllocationRequest request) {
        return service.preview(request);
    }
}
