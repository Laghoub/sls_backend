package com.ecole.gestion_scolaire.finance.controller;

import com.ecole.gestion_scolaire.finance.service.FinanceEmailService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/finance/emails")
public class FinanceEmailController {
    private final FinanceEmailService emails;
    public FinanceEmailController(FinanceEmailService emails){this.emails=emails;}

    @PostMapping("/retry-pending")
    @PreAuthorize("hasAuthority('PAIEMENT_CREER')")
    public Map<String,Integer> retry(){return Map.of("sent",emails.retryPending());}
}
