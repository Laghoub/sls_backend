package com.ecole.gestion_scolaire.finance.service;

import com.ecole.gestion_scolaire.common.dto.PageResponse;
import com.ecole.gestion_scolaire.finance.dto.cash.*;
import com.ecole.gestion_scolaire.finance.entity.CashMovement;
import com.ecole.gestion_scolaire.finance.exception.*;
import com.ecole.gestion_scolaire.finance.repository.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CashMovementService {
    private final CashMovementRepository repo;
    private final CashRegisterSessionRepository sessions;
    private final CurrentFinanceAccountService current;

    public CashMovementService(CashMovementRepository r, CashRegisterSessionRepository s, CurrentFinanceAccountService c) {
        repo = r;
        sessions = s;
        current = c;
    }

    @Transactional(readOnly = true)
    public PageResponse<CashMovementResponse> bySession(Long id, int page, int size) {
        var p = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)), Sort.by("createdAt").descending());
        return PageResponse.from(repo.findByCashRegisterSessionId(id, p).map(this::toResponse));
    }

    @Transactional
    public CashMovementResponse manual(CashMovementRequest r) {
        var s = sessions.findById(r.cashRegisterSessionId()).orElseThrow(() -> new FinanceNotFoundException("Session de caisse introuvable."));
        if (!"OPEN".equals(s.getStatus())) throw new FinanceBusinessException("La session de caisse est fermée.");
        var x = new CashMovement();
        x.setCashRegisterSessionId(r.cashRegisterSessionId());
        x.setMovementType(r.movementType().trim().toUpperCase());
        x.setDirection(r.direction().trim().toUpperCase());
        x.setAmount(r.amount());
        x.setReference(r.reference());
        x.setDescription(r.description());
        x.setCreatedBy(current.id());
        return toResponse(repo.save(x));
    }

    @Transactional
    public void payment(Long sessionId, Long paymentId, java.math.BigDecimal amount, String ref) {
        var x = new CashMovement();
        x.setCashRegisterSessionId(sessionId);
        x.setMovementType("PAYMENT");
        x.setDirection("IN");
        x.setAmount(amount);
        x.setPaymentId(paymentId);
        x.setReference(ref);
        x.setDescription("Encaissement paiement");
        x.setCreatedBy(current.id());
        repo.save(x);
    }

    @Transactional
    public void refund(Long sessionId, Long refundId, java.math.BigDecimal amount, String ref) {
        var x = new CashMovement();
        x.setCashRegisterSessionId(sessionId);
        x.setMovementType("REFUND");
        x.setDirection("OUT");
        x.setAmount(amount);
        x.setRefundId(refundId);
        x.setReference(ref);
        x.setDescription("Remboursement");
        x.setCreatedBy(current.id());
        repo.save(x);
    }

    public CashMovementResponse toResponse(CashMovement x) {
        return new CashMovementResponse(x.getId(), x.getCashRegisterSessionId(), x.getMovementType(), x.getDirection(), x.getAmount(), x.getPaymentId(), x.getRefundId(), x.getReference(), x.getDescription(), x.getCreatedBy(), x.getCreatedAt());
    }
}
