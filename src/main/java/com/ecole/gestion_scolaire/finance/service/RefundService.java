package com.ecole.gestion_scolaire.finance.service;

import com.ecole.gestion_scolaire.common.dto.PageResponse;
import com.ecole.gestion_scolaire.finance.dto.refund.*;
import com.ecole.gestion_scolaire.finance.entity.*;
import com.ecole.gestion_scolaire.finance.exception.*;
import com.ecole.gestion_scolaire.finance.integration.RegistrationPaymentService;
import com.ecole.gestion_scolaire.finance.repository.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Service
public class RefundService {
    private final RefundRepository repo;
    private final PaymentService payments;
    private final PaymentMethodService methods;
    private final CurrentFinanceAccountService current;
    private final CashRegisterSessionService sessions;
    private final CashMovementService movements;
    private final FamilyCreditRepository credits;
    private final RefundCreditReversalRepository creditReversals;
    private final PaymentAllocationRepository paymentAllocations;
    private final RefundAllocationRepository refundAllocations;
    private final StudentChargeService charges;
    private final RegistrationPaymentService registrationPayments;
    private final FinanceEmailService emails;

    public RefundService(RefundRepository r, PaymentService p, PaymentMethodService m,
                         CurrentFinanceAccountService c, CashRegisterSessionService s,
                         CashMovementService mv, FamilyCreditRepository credits,
                         RefundCreditReversalRepository creditReversals,
                         PaymentAllocationRepository paymentAllocations,
                         RefundAllocationRepository refundAllocations,
                         StudentChargeService charges,
                         RegistrationPaymentService registrationPayments,
                         FinanceEmailService emails) {
        repo=r; payments=p; methods=m; current=c; sessions=s; movements=mv;
        this.credits=credits; this.creditReversals=creditReversals;
        this.paymentAllocations=paymentAllocations; this.refundAllocations=refundAllocations;
        this.charges=charges; this.registrationPayments=registrationPayments; this.emails=emails;
    }

    @Transactional(readOnly = true)
    public PageResponse<RefundResponse> byPayment(Long id, int page, int size) {
        var p = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)), Sort.by("refundDate").descending());
        return PageResponse.from(repo.findByPaymentId(id, p).map(this::toResponse));
    }

    @Transactional
    public RefundResponse create(RefundRequest r) {
        var payment = payments.get(r.paymentId());
        if (!"VALIDATED".equals(payment.getStatus()))
            throw new FinanceBusinessException("Seul un paiement validé peut être remboursé.");

        var method = methods.get(r.paymentMethodId());
        if (!method.isActive()) throw new FinanceBusinessException("Mode de paiement inactif.");

        BigDecimal already = repo.findByPaymentId(r.paymentId(), Pageable.unpaged()).getContent().stream()
                .filter(x -> "VALIDATED".equals(x.getStatus()))
                .map(Refund::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal refundable = payment.getTotalAmount().subtract(already);
        if (r.amount().compareTo(refundable) > 0)
            throw new FinanceBusinessException("Le total remboursé dépasse le solde remboursable du paiement.");

        boolean requiresCashSession = !"VIREMENT".equalsIgnoreCase(method.getCode());
        if (requiresCashSession && r.cashRegisterSessionId() == null)
            throw new FinanceBusinessException("Une session de caisse ouverte est obligatoire pour ce remboursement.");
        if (r.cashRegisterSessionId() != null) sessions.getOpen(r.cashRegisterSessionId());

        Long uid = current.id();
        var refund = new Refund();
        refund.setPaymentId(r.paymentId()); refund.setAmount(r.amount()); refund.setPaymentMethodId(r.paymentMethodId());
        refund.setReason(r.reason().trim()); refund.setStatus("VALIDATED"); refund.setCreatedBy(uid); refund.setValidatedBy(uid);
        refund.setValidatedAt(OffsetDateTime.now()); refund = repo.save(refund);

        var reversal = applyFinancialReversal(refund, payment);

        if (r.cashRegisterSessionId() != null)
            movements.refund(r.cashRegisterSessionId(), refund.getId(), refund.getAmount(), payment.getPaymentNumber());

        emails.queueRefundReceipt(refund.getId(), payment.getId(), refund.getAmount(), reversal.credit(), reversal.allocations());
        return toResponse(refund);
    }

    /**
     * Répare les remboursements créés avant V14 : ils avaient bien créé la sortie de caisse,
     * mais n'avaient pas diminué l'avoir ni contre-passé les ventilations.
     */
    @Transactional
    public int reconcileLegacyRefunds() {
        int count = 0;
        for (var refund : repo.findByStatusOrderByRefundDateAscIdAsc("VALIDATED")) {
            if (refundAllocations.existsByRefundId(refund.getId()) || creditReversals.existsByRefundId(refund.getId())) continue;
            var payment = payments.get(refund.getPaymentId());
            applyFinancialReversal(refund, payment);
            count++;
        }
        return count;
    }

    private ReversalTotals applyFinancialReversal(Refund refund, Payment payment) {
        if (refundAllocations.existsByRefundId(refund.getId()) || creditReversals.existsByRefundId(refund.getId())) {
            BigDecimal c = creditReversals.findByRefundIdOrderByIdAsc(refund.getId()).stream().map(RefundCreditReversal::getAmount).reduce(BigDecimal.ZERO,BigDecimal::add);
            BigDecimal a = refundAllocations.findByRefundIdOrderByIdAsc(refund.getId()).stream().map(RefundAllocation::getAmount).reduce(BigDecimal.ZERO,BigDecimal::add);
            return new ReversalTotals(c,a);
        }

        BigDecimal remaining = refund.getAmount();
        BigDecimal creditReversed = BigDecimal.ZERO;
        BigDecimal allocationReversed = BigDecimal.ZERO;

        // Priorité au reliquat qui n'avait pas encore été affecté : l'avoir familial.
        for (var credit : credits.findBySourcePaymentIdOrderByCreatedAtAsc(payment.getId())) {
            if (remaining.signum() <= 0) break;
            if (credit.getRemainingAmount() == null || credit.getRemainingAmount().signum() <= 0) continue;
            BigDecimal take = remaining.min(credit.getRemainingAmount());
            credit.setRemainingAmount(credit.getRemainingAmount().subtract(take));
            // Un reliquat encore positif reste utilisable. Zéro signifie qu'il a été entièrement absorbé/remboursé.
            credit.setStatus(credit.getRemainingAmount().signum() == 0 ? "REFUNDED" : "PARTIALLY_USED");
            credits.save(credit);
            var cr = new RefundCreditReversal(); cr.setRefundId(refund.getId()); cr.setFamilyCreditId(credit.getId()); cr.setAmount(take); creditReversals.save(cr);
            remaining = remaining.subtract(take); creditReversed = creditReversed.add(take);
        }

        // Puis on contre-passe les allocations du paiement (les plus récentes d'abord).
        Set<Long> affectedCharges = new LinkedHashSet<>();
        for (var allocation : paymentAllocations.findByPaymentIdOrderByIdDesc(payment.getId())) {
            if (remaining.signum() <= 0) break;
            BigDecimal alreadyReversed = refundAllocations.findByPaymentAllocationId(allocation.getId()).stream()
                    .map(RefundAllocation::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal active = allocation.getAmount().subtract(alreadyReversed).max(BigDecimal.ZERO);
            if (active.signum() <= 0) continue;
            BigDecimal take = remaining.min(active);
            var ar = new RefundAllocation(); ar.setRefundId(refund.getId()); ar.setPaymentAllocationId(allocation.getId());
            ar.setStudentChargeId(allocation.getStudentChargeId()); ar.setAmount(take); refundAllocations.save(ar);
            affectedCharges.add(allocation.getStudentChargeId());
            remaining = remaining.subtract(take); allocationReversed = allocationReversed.add(take);
        }

        if (remaining.signum() > 0)
            throw new FinanceBusinessException("Impossible de justifier entièrement le remboursement avec l'avoir et les ventilations du paiement.");

        for (Long chargeId : affectedCharges) {
            var charge = charges.get(chargeId);
            charges.refreshStatus(chargeId);
            if (charge.getRegistrationCaseId() != null) registrationPayments.refresh(charge.getRegistrationCaseId());
        }
        return new ReversalTotals(creditReversed, allocationReversed);
    }

    public RefundResponse toResponse(Refund x) {
        BigDecimal credit = creditReversals.findByRefundIdOrderByIdAsc(x.getId()).stream().map(RefundCreditReversal::getAmount).reduce(BigDecimal.ZERO,BigDecimal::add);
        BigDecimal allocations = refundAllocations.findByRefundIdOrderByIdAsc(x.getId()).stream().map(RefundAllocation::getAmount).reduce(BigDecimal.ZERO,BigDecimal::add);
        return new RefundResponse(x.getId(), x.getPaymentId(), x.getRefundDate(), x.getAmount(), x.getPaymentMethodId(), x.getReason(), x.getStatus(), x.getCreatedBy(), x.getValidatedBy(), x.getValidatedAt(), x.getCreatedAt(), credit, allocations);
    }

    private record ReversalTotals(BigDecimal credit, BigDecimal allocations) {}
}
