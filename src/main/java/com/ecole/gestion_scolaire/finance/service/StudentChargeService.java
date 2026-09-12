package com.ecole.gestion_scolaire.finance.service;

import com.ecole.gestion_scolaire.common.dto.PageResponse;
import com.ecole.gestion_scolaire.finance.dto.charge.*;
import com.ecole.gestion_scolaire.finance.entity.StudentCharge;
import com.ecole.gestion_scolaire.finance.enums.ChargeStatus;
import com.ecole.gestion_scolaire.finance.exception.*;
import com.ecole.gestion_scolaire.finance.repository.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Service
public class StudentChargeService {
    private final StudentChargeRepository repo;
    private final FeeTypeRepository fees;
    private final TariffRepository tariffs;
    private final FinanceReferenceValidator refs;
    private final PaymentAllocationService allocations;
    private final ChargeCalculationService calculator;
    private final DiscountCalculationService discounts;

    public StudentChargeService(StudentChargeRepository r, FeeTypeRepository f, TariffRepository t, FinanceReferenceValidator v, PaymentAllocationService a, ChargeCalculationService c, DiscountCalculationService discounts) {
        repo = r;
        fees = f;
        tariffs = t;
        refs = v;
        allocations = a;
        calculator = c;
        this.discounts = discounts;
    }

    @Transactional(readOnly = true)
    public StudentCharge get(Long id) {
        return repo.findById(id).orElseThrow(() -> new FinanceNotFoundException("Créance introuvable : " + id));
    }

    @Transactional(readOnly = true)
    public PageResponse<StudentChargeResponse> search(Long registrationId, Long enrollmentId, int page, int size) {
        var p = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)), Sort.by("createdAt").descending());
        Page<StudentCharge> x = registrationId != null ? repo.findByRegistrationCaseId(registrationId, p) : enrollmentId != null ? repo.findByStudentEnrollmentId(enrollmentId, p) : repo.findAll(p);
        return PageResponse.from(x.map(this::toResponse));
    }

    @Transactional
    public StudentChargeResponse create(StudentChargeCreateRequest r) {
        if ((r.registrationCaseId() == null) == (r.studentEnrollmentId() == null))
            throw new FinanceBusinessException("Une créance doit appartenir soit à un dossier d'inscription, soit à une scolarisation, exclusivement.");
        refs.registration(r.registrationCaseId());
        refs.enrollment(r.studentEnrollmentId());
        if (!fees.existsById(r.feeTypeId())) throw new FinanceNotFoundException("Type de frais introuvable.");
        if (r.tariffId() != null && !tariffs.existsById(r.tariffId()))
            throw new FinanceNotFoundException("Tarif introuvable.");
        var x = new StudentCharge();
        x.setRegistrationCaseId(r.registrationCaseId());
        x.setStudentEnrollmentId(r.studentEnrollmentId());
        x.setFeeTypeId(r.feeTypeId());
        x.setTariffId(r.tariffId());
        x.setLabel(r.label().trim());
        BigDecimal requestedDiscount = r.discountAmount() == null ? BigDecimal.ZERO : r.discountAmount();
        BigDecimal automaticDiscount = BigDecimal.ZERO;
        if (r.studentEnrollmentId() != null) {
            var discountDate = r.billingPeriodStart() != null ? r.billingPeriodStart() : r.dueDate();
            automaticDiscount = discounts.calculate(r.studentEnrollmentId(), r.feeTypeId(), r.originalAmount(), discountDate).discountAmount();
        }
        BigDecimal effectiveDiscount = requestedDiscount.max(automaticDiscount).min(r.originalAmount());
        x.setOriginalAmount(r.originalAmount());
        x.setDiscountAmount(effectiveDiscount);
        x.setFinalAmount(calculator.finalAmount(r.originalAmount(), effectiveDiscount));
        x.setDueDate(r.dueDate());
        x.setBillingPeriodStart(r.billingPeriodStart());
        x.setBillingPeriodEnd(r.billingPeriodEnd());
        x.setStatus(ChargeStatus.DUE.name());
        return toResponse(repo.save(x));
    }

    @Transactional
    public StudentChargeResponse cancel(Long id, String reason) {
        var x = get(id);
        if (allocations.paidForCharge(id).signum() > 0)
            throw new FinanceBusinessException("Impossible d'annuler une créance déjà encaissée.");
        x.setStatus(ChargeStatus.CANCELLED.name());
        x.setCancelledAt(OffsetDateTime.now());
        x.setCancellationReason(reason);
        return toResponse(repo.save(x));
    }

    @Transactional
    public void refreshStatus(Long id) {
        var x = get(id);
        if (ChargeStatus.CANCELLED.name().equals(x.getStatus())) return;
        var paid = allocations.paidForCharge(id);
        x.setStatus(paid.signum() == 0 ? ChargeStatus.DUE.name() : paid.compareTo(x.getFinalAmount()) >= 0 ? ChargeStatus.PAID.name() : ChargeStatus.PARTIALLY_PAID.name());
        repo.save(x);
    }

    @Transactional(readOnly = true)
    public StudentChargeResponse toResponse(StudentCharge x) {
        var paid = allocations.paidForCharge(x.getId());
        var remaining = x.getFinalAmount().subtract(paid).max(BigDecimal.ZERO);
        return new StudentChargeResponse(x.getId(), x.getStudentEnrollmentId(), x.getRegistrationCaseId(), x.getFeeTypeId(), x.getTariffId(), x.getLabel(), x.getOriginalAmount(), x.getDiscountAmount(), x.getFinalAmount(), paid, remaining, x.getDueDate(), x.getBillingPeriodStart(), x.getBillingPeriodEnd(), x.getStatus(), x.getCreatedAt());
    }
}
