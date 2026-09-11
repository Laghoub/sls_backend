package com.ecole.gestion_scolaire.finance.service;

import com.ecole.gestion_scolaire.finance.dto.payment.*;
import com.ecole.gestion_scolaire.finance.entity.PaymentAllocation;
import com.ecole.gestion_scolaire.finance.repository.PaymentAllocationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PaymentAllocationService {
    private final PaymentAllocationRepository repo;

    public PaymentAllocationService(PaymentAllocationRepository r) {
        repo = r;
    }

    @Transactional(readOnly = true)
    public List<PaymentAllocationResponse> byPayment(Long id) {
        return repo.findByPaymentIdOrderByIdAsc(id).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public BigDecimal paidForCharge(Long id) {
        return repo.findByStudentChargeId(id).stream().map(PaymentAllocation::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional
    public PaymentAllocationResponse create(Long paymentId, PaymentAllocationRequest r) {
        var x = new PaymentAllocation();
        x.setPaymentId(paymentId);
        x.setStudentChargeId(r.studentChargeId());
        x.setAmount(r.amount());
        return toResponse(repo.save(x));
    }

    public PaymentAllocationResponse toResponse(PaymentAllocation x) {
        return new PaymentAllocationResponse(x.getId(), x.getPaymentId(), x.getStudentChargeId(), x.getAmount(), x.getCreatedAt());
    }
}
