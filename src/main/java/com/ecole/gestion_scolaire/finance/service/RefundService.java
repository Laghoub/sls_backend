package com.ecole.gestion_scolaire.finance.service;

import com.ecole.gestion_scolaire.common.dto.PageResponse;
import com.ecole.gestion_scolaire.finance.dto.refund.*;
import com.ecole.gestion_scolaire.finance.entity.Refund;
import com.ecole.gestion_scolaire.finance.exception.*;
import com.ecole.gestion_scolaire.finance.repository.RefundRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Service
public class RefundService {
    private final RefundRepository repo;
    private final PaymentService payments;
    private final PaymentMethodService methods;
    private final CurrentFinanceAccountService current;
    private final CashRegisterSessionService sessions;
    private final CashMovementService movements;

    public RefundService(RefundRepository r, PaymentService p, PaymentMethodService m, CurrentFinanceAccountService c, CashRegisterSessionService s, CashMovementService mv) {
        repo = r;
        payments = p;
        methods = m;
        current = c;
        sessions = s;
        movements = mv;
    }

    @Transactional(readOnly = true)
    public PageResponse<RefundResponse> byPayment(Long id, int page, int size) {
        var p = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)), Sort.by("refundDate").descending());
        return PageResponse.from(repo.findByPaymentId(id, p).map(this::toResponse));
    }

    @Transactional
    public RefundResponse create(RefundRequest r) {
        var p = payments.get(r.paymentId());
        methods.get(r.paymentMethodId());
        BigDecimal already = repo.findByPaymentId(r.paymentId(), Pageable.unpaged()).getContent().stream().filter(x -> "VALIDATED".equals(x.getStatus())).map(Refund::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (already.add(r.amount()).compareTo(p.getTotalAmount()) > 0)
            throw new FinanceBusinessException("Le total remboursé dépasse le paiement initial.");
        if (r.cashRegisterSessionId() != null) sessions.getOpen(r.cashRegisterSessionId());
        Long uid = current.id();
        var x = new Refund();
        x.setPaymentId(r.paymentId());
        x.setAmount(r.amount());
        x.setPaymentMethodId(r.paymentMethodId());
        x.setReason(r.reason().trim());
        x.setStatus("VALIDATED");
        x.setCreatedBy(uid);
        x.setValidatedBy(uid);
        x.setValidatedAt(OffsetDateTime.now());
        x = repo.save(x);
        if (r.cashRegisterSessionId() != null)
            movements.refund(r.cashRegisterSessionId(), x.getId(), x.getAmount(), p.getPaymentNumber());
        return toResponse(x);
    }

    public RefundResponse toResponse(Refund x) {
        return new RefundResponse(x.getId(), x.getPaymentId(), x.getRefundDate(), x.getAmount(), x.getPaymentMethodId(), x.getReason(), x.getStatus(), x.getCreatedBy(), x.getValidatedBy(), x.getValidatedAt(), x.getCreatedAt());
    }
}
