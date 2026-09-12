package com.ecole.gestion_scolaire.finance.service;

import com.ecole.gestion_scolaire.common.dto.PageResponse;
import com.ecole.gestion_scolaire.finance.dto.payment.*;
import com.ecole.gestion_scolaire.finance.dto.credit.FamilyCreditResponse;
import com.ecole.gestion_scolaire.finance.entity.Payment;
import com.ecole.gestion_scolaire.finance.exception.*;
import com.ecole.gestion_scolaire.finance.integration.RegistrationPaymentService;
import com.ecole.gestion_scolaire.finance.repository.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

@Service
public class PaymentService {
    private final PaymentRepository repo;
    private final PaymentMethodService methods;
    private final StudentChargeService charges;
    private final PaymentAllocationService allocations;
    private final FamilyCreditService credits;
    private final CashRegisterSessionService sessions;
    private final CashMovementService movements;
    private final CurrentFinanceAccountService current;
    private final FinanceReferenceValidator refs;
    private final PaymentNumberGenerator numbers;
    private final RegistrationPaymentService registration;
    private final CollectionReferenceService collection;
    private final FinanceEmailService emails;

    public PaymentService(PaymentRepository r, PaymentMethodService m, StudentChargeService c, PaymentAllocationService a, FamilyCreditService f, CashRegisterSessionService s, CashMovementService mv, CurrentFinanceAccountService u, FinanceReferenceValidator v, PaymentNumberGenerator n, RegistrationPaymentService i, CollectionReferenceService collection, FinanceEmailService emails) {
        repo = r;
        methods = m;
        charges = c;
        allocations = a;
        credits = f;
        sessions = s;
        movements = mv;
        current = u;
        refs = v;
        numbers = n;
        registration = i;
        this.collection = collection;
        this.emails = emails;
    }

    @Transactional(readOnly = true)
    public Payment get(Long id) {
        return repo.findById(id).orElseThrow(() -> new FinanceNotFoundException("Paiement introuvable : " + id));
    }

    @Transactional(readOnly = true)
    public PageResponse<PaymentResponse> search(Long guardianId, int page, int size) {
        var p = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)), Sort.by("paymentDate").descending());
        var x = guardianId == null ? repo.findAll(p) : repo.findByGuardianId(guardianId, p);
        return PageResponse.from(x.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public PaymentDetailResponse detail(Long id) {
        var x = get(id);
        return new PaymentDetailResponse(toResponse(x), allocations.byPayment(id), null);
    }

    @Transactional
    public PaymentDetailResponse create(PaymentCreateRequest r) {
        refs.guardian(r.guardianId());
        var method = methods.get(r.paymentMethodId());
        if (!method.isActive()) throw new FinanceBusinessException("Mode de paiement inactif.");
        if (r.allocations() == null) throw new FinanceBusinessException("La liste des ventilations est obligatoire.");
        var ids = new HashSet<Long>();
        BigDecimal allocated = BigDecimal.ZERO;
        Map<Long, Long> registrations = new HashMap<>();
        for (var a : r.allocations()) {
            if (!ids.add(a.studentChargeId()))
                throw new FinanceBusinessException("Une créance ne peut apparaître qu'une fois dans le paiement.");
            var c = charges.get(a.studentChargeId());
            collection.assertGuardianCanPayCharge(r.guardianId(), c);
            if ("CANCELLED".equals(c.getStatus()) || "PAID".equals(c.getStatus()))
                throw new FinanceBusinessException("Créance non payable : " + c.getId());
            BigDecimal remaining = charges.toResponse(c).remainingAmount();
            if (a.amount().compareTo(remaining) > 0)
                throw new FinanceBusinessException("Montant supérieur au reste dû pour la créance " + c.getId());
            allocated = allocated.add(a.amount());
            if (c.getRegistrationCaseId() != null)
                registrations.put(c.getRegistrationCaseId(), c.getRegistrationCaseId());
        }
        if (allocated.compareTo(r.totalAmount()) > 0)
            throw new FinanceBusinessException("La ventilation dépasse le montant du paiement.");
        boolean requiresCashSession = !"VIREMENT".equalsIgnoreCase(method.getCode());
        if (requiresCashSession && r.cashRegisterSessionId() == null)
            throw new FinanceBusinessException("Une session de caisse ouverte est obligatoire pour ce mode de paiement.");
        if (r.cashRegisterSessionId() != null) sessions.getOpen(r.cashRegisterSessionId());
        Long uid = current.id();
        var x = new Payment();
        x.setPaymentNumber(numbers.next());
        x.setGuardianId(r.guardianId());
        x.setPaymentDate(r.paymentDate() == null ? OffsetDateTime.now() : r.paymentDate());
        x.setPaymentMethodId(r.paymentMethodId());
        x.setTotalAmount(r.totalAmount());
        x.setStatus("VALIDATED");
        x.setCashierId(uid);
        x.setCashRegisterSessionId(r.cashRegisterSessionId());
        x.setExternalReference(r.externalReference());
        x.setNotes(r.notes());
        x.setValidatedAt(OffsetDateTime.now());
        x.setValidatedBy(uid);
        x = repo.save(x);
        for (var a : r.allocations()) {
            allocations.create(x.getId(), a);
            charges.refreshStatus(a.studentChargeId());
        }
        FamilyCreditResponse credit = credits.create(r.guardianId(), x.getId(), r.totalAmount().subtract(allocated));
        if (r.cashRegisterSessionId() != null)
            movements.payment(r.cashRegisterSessionId(), x.getId(), x.getTotalAmount(), x.getPaymentNumber());
        for (Long registrationId : registrations.keySet()) registration.refresh(registrationId);
        emails.queuePaymentReceipt(x.getId());
        return new PaymentDetailResponse(toResponse(x), allocations.byPayment(x.getId()), credit);
    }

    @Transactional
    public PaymentResponse cancel(Long id, String reason) {
        var x = get(id);
        if ("CANCELLED".equals(x.getStatus())) throw new FinanceBusinessException("Paiement déjà annulé.");
        if (!allocations.byPayment(id).isEmpty())
            throw new FinanceBusinessException("L'annulation d'un paiement ventilé nécessite une opération de remboursement/contrepassation. Utilisez le module Remboursement.");
        x.setStatus("CANCELLED");
        x.setCancelledAt(OffsetDateTime.now());
        x.setCancelledBy(current.id());
        x.setCancellationReason(reason);
        return toResponse(repo.save(x));
    }

    public PaymentResponse toResponse(Payment x) {
        return new PaymentResponse(x.getId(), x.getPaymentNumber(), x.getGuardianId(), x.getPaymentDate(), x.getPaymentMethodId(), x.getTotalAmount(), x.getStatus(), x.getCashierId(), x.getCashRegisterSessionId(), x.getExternalReference(), x.getNotes(), x.getCreatedAt());
    }
}
