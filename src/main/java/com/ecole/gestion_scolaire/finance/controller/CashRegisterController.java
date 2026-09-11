package com.ecole.gestion_scolaire.finance.controller;

import com.ecole.gestion_scolaire.finance.dto.cash.*;
import com.ecole.gestion_scolaire.finance.service.*;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.ecole.gestion_scolaire.common.dto.PageResponse;

@RestController
@RequestMapping("/api/finance/cash-registers")
public class CashRegisterController {
    private final CashRegisterService registers;
    private final CashRegisterSessionService sessions;

    public CashRegisterController(CashRegisterService r, CashRegisterSessionService s) {
        registers = r;
        sessions = s;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CAISSE_CONSULTER')")
    public List<CashRegisterResponse> all() {
        return registers.all();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CAISSE_GERER')")
    public CashRegisterResponse create(@Valid @RequestBody CashRegisterRequest r) {
        return registers.save(null, r);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CAISSE_GERER')")
    public CashRegisterResponse update(@PathVariable Long id, @Valid @RequestBody CashRegisterRequest r) {
        return registers.save(id, r);
    }

    @GetMapping("/sessions/current")
    @PreAuthorize("hasAuthority('CAISSE_CONSULTER')")
    public CashRegisterSessionResponse current() { return sessions.currentForUser(); }

    @GetMapping("/{id}/sessions")
    @PreAuthorize("hasAuthority('CAISSE_CONSULTER')")
    public PageResponse<CashRegisterSessionResponse> history(@PathVariable Long id, @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size) { return sessions.history(id, page, size); }

    @PostMapping("/sessions/open")
    @PreAuthorize("hasAuthority('CAISSE_OUVRIR')")
    public CashRegisterSessionResponse open(@Valid @RequestBody CashSessionOpenRequest r) {
        return sessions.open(r);
    }

    @PostMapping("/sessions/{id}/close")
    @PreAuthorize("hasAuthority('CAISSE_FERMER')")
    public CashRegisterSessionResponse close(@PathVariable Long id, @Valid @RequestBody CashSessionCloseRequest r) {
        return sessions.close(id, r);
    }
}
