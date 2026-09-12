package com.ecole.gestion_scolaire.finance.service;

import com.ecole.gestion_scolaire.finance.dto.payment.*;
import com.ecole.gestion_scolaire.finance.entity.PaymentAllocation;
import com.ecole.gestion_scolaire.finance.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PaymentAllocationService {
    private final PaymentAllocationRepository repo;
    private final FamilyCreditUsageRepository creditUsages;
    private final RefundAllocationRepository refundAllocations;

    public PaymentAllocationService(PaymentAllocationRepository r, FamilyCreditUsageRepository creditUsages,
                                    RefundAllocationRepository refundAllocations) {
        repo = r;
        this.creditUsages = creditUsages;
        this.refundAllocations = refundAllocations;
    }

    @Transactional(readOnly = true)
    public List<PaymentAllocationResponse> byPayment(Long id) {
        return repo.findByPaymentIdOrderByIdAsc(id).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public BigDecimal paidForCharge(Long id) {
        BigDecimal paymentAllocations = repo.findByStudentChargeId(id).stream()
                .map(a -> a.getAmount().subtract(refundedForAllocation(a.getId())).max(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal directCredits = creditUsages.findByStudentChargeIdAndPaymentAllocationIdIsNull(id).stream()
                .map(com.ecole.gestion_scolaire.finance.entity.FamilyCreditUsage::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return paymentAllocations.add(directCredits);
    }

    @Transactional(readOnly = true)
    public BigDecimal refundedForAllocation(Long allocationId) {
        return refundAllocations.findByPaymentAllocationId(allocationId).stream()
                .map(com.ecole.gestion_scolaire.finance.entity.RefundAllocation::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
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
