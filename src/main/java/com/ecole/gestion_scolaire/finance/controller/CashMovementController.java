package com.ecole.gestion_scolaire.finance.controller;

import com.ecole.gestion_scolaire.common.dto.PageResponse;
import com.ecole.gestion_scolaire.finance.dto.cash.*;
import com.ecole.gestion_scolaire.finance.service.CashMovementService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/finance/cash-movements")
public class CashMovementController {
    private final CashMovementService s;

    public CashMovementController(CashMovementService x) {
        s = x;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CAISSE_CONSULTER')")
    public PageResponse<CashMovementResponse> all(@RequestParam Long sessionId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return s.bySession(sessionId, page, size);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CAISSE_MOUVEMENT_GERER')")
    public CashMovementResponse create(@Valid @RequestBody CashMovementRequest r) {
        return s.manual(r);
    }
}
