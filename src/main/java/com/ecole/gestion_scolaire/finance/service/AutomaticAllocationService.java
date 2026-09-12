package com.ecole.gestion_scolaire.finance.service;

import com.ecole.gestion_scolaire.finance.dto.allocation.*;
import com.ecole.gestion_scolaire.finance.dto.collection.StudentOpenChargeResponse;
import com.ecole.gestion_scolaire.finance.exception.FinanceBusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
public class AutomaticAllocationService {
    private final CollectionReferenceService collection;

    public AutomaticAllocationService(CollectionReferenceService collection) {
        this.collection = collection;
    }

    /**
     * Prépare une proposition sans créer de paiement.
     *
     * Ordre familial : enfant 1 -> enfant 2 -> ... -> enfant 1 -> enfant 2.
     * Chaque enfant possède sa propre file de créances, triée de la plus ancienne
     * à la plus récente. Cela inclut toutes les créances ouvertes : scolarité,
     * livres, cantine, transport, etc. Une créance est soldée avant de passer à
     * la suivante ; seule la dernière ligne peut donc être partielle.
     */
    @Transactional(readOnly = true)
    public AutomaticAllocationResponse preview(AutomaticAllocationRequest request) {
        if (request.amount() == null || request.amount().signum() <= 0)
            throw new FinanceBusinessException("Le montant à ventiler doit être strictement positif.");

        List<StudentOpenChargeResponse> open = collection.openChargesForGuardian(request.guardianId(), request.schoolYearId());
        Map<Long, List<StudentOpenChargeResponse>> byStudent = new TreeMap<>();
        for (var charge : open) {
            if (charge.remainingAmount() == null || charge.remainingAmount().signum() <= 0) continue;
            byStudent.computeIfAbsent(charge.studentId(), k -> new ArrayList<>()).add(charge);
        }
        Comparator<StudentOpenChargeResponse> oldestFirst = Comparator
                .comparing(this::effectivePeriod, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(StudentOpenChargeResponse::dueDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(StudentOpenChargeResponse::chargeId);
        byStudent.values().forEach(list -> list.sort(oldestFirst));

        List<StudentOpenChargeResponse> roundRobin = new ArrayList<>();
        int round = 0;
        boolean added;
        do {
            added = false;
            for (var queue : byStudent.values()) {
                if (round < queue.size()) {
                    roundRobin.add(queue.get(round));
                    added = true;
                }
            }
            round++;
        } while (added);

        BigDecimal available = request.amount();
        BigDecimal allocated = BigDecimal.ZERO;
        List<AutomaticAllocationLineResponse> lines = new ArrayList<>();
        for (var charge : roundRobin) {
            if (available.signum() <= 0) break;
            BigDecimal amount = available.min(charge.remainingAmount());
            BigDecimal after = charge.remainingAmount().subtract(amount);
            lines.add(new AutomaticAllocationLineResponse(
                    charge.chargeId(), charge.studentId(), charge.studentNumber(), charge.studentLastName(), charge.studentFirstName(),
                    charge.label(), charge.dueDate(), charge.billingPeriodStart(), charge.billingPeriodEnd(),
                    charge.remainingAmount(), amount, after, after.signum() == 0));
            allocated = allocated.add(amount);
            available = available.subtract(amount);
        }
        return new AutomaticAllocationResponse(request.guardianId(), request.schoolYearId(), request.amount(),
                allocated, request.amount().subtract(allocated), lines.size(), List.copyOf(lines));
    }

    private LocalDate effectivePeriod(StudentOpenChargeResponse charge) {
        return charge.billingPeriodStart() != null ? charge.billingPeriodStart() : charge.dueDate();
    }
}
