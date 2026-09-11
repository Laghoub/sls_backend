package com.ecole.gestion_scolaire.finance.controller;

import com.ecole.gestion_scolaire.common.dto.PageResponse;
import com.ecole.gestion_scolaire.finance.dto.refund.*;
import com.ecole.gestion_scolaire.finance.service.RefundService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/finance/refunds")
public class RefundController {
    private final RefundService s;

    public RefundController(RefundService x) {
        s = x;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('REMBOURSEMENT_CONSULTER')")
    public PageResponse<RefundResponse> all(@RequestParam Long paymentId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return s.byPayment(paymentId, page, size);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('REMBOURSEMENT_CREER')")
    public RefundResponse create(@Valid @RequestBody RefundRequest r) {
        return s.create(r);
    }
}
