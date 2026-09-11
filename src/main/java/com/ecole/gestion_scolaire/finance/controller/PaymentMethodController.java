package com.ecole.gestion_scolaire.finance.controller;

import com.ecole.gestion_scolaire.finance.entity.PaymentMethod;
import com.ecole.gestion_scolaire.finance.service.PaymentMethodService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/finance/payment-methods")
public class PaymentMethodController {
    private final PaymentMethodService s;

    public PaymentMethodController(PaymentMethodService x) {
        s = x;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PAIEMENT_CONSULTER')")
    public List<PaymentMethod> all() {
        return s.all();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('FINANCE_PARAMETRER')")
    public PaymentMethod create(@RequestBody Map<String, Object> b) {
        return s.save(null, (String) b.get("code"), (String) b.get("name"), Boolean.TRUE.equals(b.get("active")));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('FINANCE_PARAMETRER')")
    public PaymentMethod update(@PathVariable Long id, @RequestBody Map<String, Object> b) {
        return s.save(id, (String) b.get("code"), (String) b.get("name"), Boolean.TRUE.equals(b.get("active")));
    }
}
