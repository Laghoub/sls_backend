package com.ecole.gestion_scolaire.finance.controller;

import com.ecole.gestion_scolaire.finance.dto.receipt.PaymentReceiptResponse;
import com.ecole.gestion_scolaire.finance.service.PaymentReceiptService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/finance/payment-receipts")
public class PaymentReceiptController {
    private final PaymentReceiptService service;
    public PaymentReceiptController(PaymentReceiptService s){service=s;}
    @GetMapping("/{paymentId}")
    @PreAuthorize("hasAuthority('PAIEMENT_CONSULTER')")
    public PaymentReceiptResponse receipt(@PathVariable Long paymentId){return service.get(paymentId);}
}
