package com.ecole.gestion_scolaire.finance.controller;

import com.ecole.gestion_scolaire.common.dto.PageResponse;
import com.ecole.gestion_scolaire.finance.dto.payment.*;
import com.ecole.gestion_scolaire.finance.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/finance/payments")
public class PaymentController {
    private final PaymentService s;

    public PaymentController(PaymentService x) {
        s = x;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PAIEMENT_CONSULTER')")
    public PageResponse<PaymentResponse> all(@RequestParam(required = false) Long guardianId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return s.search(guardianId, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PAIEMENT_CONSULTER')")
    public PaymentDetailResponse one(@PathVariable Long id) {
        return s.detail(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PAIEMENT_CREER')")
    public PaymentDetailResponse create(@Valid @RequestBody PaymentCreateRequest r) {
        return s.create(r);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('PAIEMENT_ANNULER')")
    public PaymentResponse cancel(@PathVariable Long id, @Valid @RequestBody PaymentCancelRequest r) {
        return s.cancel(id, r.reason());
    }
}
