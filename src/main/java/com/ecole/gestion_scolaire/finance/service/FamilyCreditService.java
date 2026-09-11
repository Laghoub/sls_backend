package com.ecole.gestion_scolaire.finance.service;

import com.ecole.gestion_scolaire.common.dto.PageResponse;
import com.ecole.gestion_scolaire.finance.dto.credit.*;
import com.ecole.gestion_scolaire.finance.dto.payment.PaymentAllocationRequest;
import com.ecole.gestion_scolaire.finance.entity.*;
import com.ecole.gestion_scolaire.finance.exception.*;
import com.ecole.gestion_scolaire.finance.integration.RegistrationPaymentService;
import com.ecole.gestion_scolaire.finance.repository.*;
import com.ecole.gestion_scolaire.registration.repository.*;
import com.ecole.gestion_scolaire.student.repository.StudentGuardianRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class FamilyCreditService {
    private final FamilyCreditRepository repo;
    private final FamilyCreditUsageRepository usages;
    private final StudentChargeRepository charges;
    private final PaymentAllocationService allocations;
    private final PaymentRepository payments;
    private final StudentEnrollmentRepository enrollments;
    private final RegistrationCaseRepository registrations;
    private final StudentGuardianRepository guardianLinks;
    private final RegistrationPaymentService registrationPayments;

    public FamilyCreditService(FamilyCreditRepository r, FamilyCreditUsageRepository u, StudentChargeRepository c,
                               PaymentAllocationService a, PaymentRepository p, StudentEnrollmentRepository e,
                               RegistrationCaseRepository rc, StudentGuardianRepository gl, RegistrationPaymentService rp) {
        repo=r; usages=u; charges=c; allocations=a; payments=p; enrollments=e; registrations=rc; guardianLinks=gl; registrationPayments=rp;
    }

    @Transactional
    public FamilyCreditResponse create(Long guardianId, Long paymentId, BigDecimal amount) {
        if (amount.signum() <= 0) return null;
        var x = new FamilyCredit();
        x.setGuardianId(guardianId); x.setSourcePaymentId(paymentId); x.setInitialAmount(amount); x.setRemainingAmount(amount); x.setStatus("AVAILABLE");
        return toResponse(repo.save(x));
    }

    @Transactional(readOnly = true)
    public PageResponse<FamilyCreditResponse> byGuardian(Long id, int page, int size) {
        var p = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)), Sort.by("createdAt").descending());
        return PageResponse.from(repo.findByGuardianId(id, p).map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public List<FamilyCreditUsageResponse> usages(Long creditId) {
        return usages.findByFamilyCreditIdOrderByCreatedAtDesc(creditId).stream().map(this::toUsage).toList();
    }

    @Transactional
    public FamilyCreditUsageResponse apply(Long creditId, FamilyCreditApplyRequest request) {
        var credit = repo.findById(creditId).orElseThrow(() -> new FinanceNotFoundException("Avoir familial introuvable."));
        if (!"AVAILABLE".equals(credit.getStatus()) && !"PARTIALLY_USED".equals(credit.getStatus()))
            throw new FinanceBusinessException("Cet avoir n'est plus disponible.");
        if (request.amount().compareTo(credit.getRemainingAmount()) > 0)
            throw new FinanceBusinessException("Le montant dépasse le solde disponible de l'avoir.");
        var charge = charges.findById(request.studentChargeId()).orElseThrow(() -> new FinanceNotFoundException("Créance introuvable."));
        if ("CANCELLED".equals(charge.getStatus()) || "PAID".equals(charge.getStatus()))
            throw new FinanceBusinessException("Cette créance n'est pas payable.");
        ensureGuardianOwnsCharge(credit.getGuardianId(), charge);
        var sourcePayment = payments.findById(credit.getSourcePaymentId()).orElseThrow(() -> new FinanceNotFoundException("Paiement source introuvable."));
        if (!"VALIDATED".equals(sourcePayment.getStatus())) throw new FinanceBusinessException("Le paiement source de l'avoir n'est pas valide.");

        BigDecimal alreadyPaid = allocations.paidForCharge(charge.getId());
        BigDecimal remaining = charge.getFinalAmount().subtract(alreadyPaid).max(BigDecimal.ZERO);
        if (request.amount().compareTo(remaining) > 0) throw new FinanceBusinessException("Le montant dépasse le reste dû de la créance.");

        var allocation = allocations.create(sourcePayment.getId(), new PaymentAllocationRequest(charge.getId(), request.amount()));
        credit.setRemainingAmount(credit.getRemainingAmount().subtract(request.amount()));
        credit.setStatus(credit.getRemainingAmount().signum()==0 ? "USED" : "PARTIALLY_USED");
        repo.save(credit);

        var usage = new FamilyCreditUsage();
        usage.setFamilyCreditId(credit.getId()); usage.setGuardianId(credit.getGuardianId()); usage.setStudentChargeId(charge.getId());
        usage.setPaymentAllocationId(allocation.id()); usage.setAmount(request.amount()); usage = usages.save(usage);

        refreshCharge(charge);
        if (charge.getRegistrationCaseId()!=null) registrationPayments.refresh(charge.getRegistrationCaseId());
        return toUsage(usage);
    }

    private void refreshCharge(StudentCharge charge) {
        BigDecimal paid = allocations.paidForCharge(charge.getId());
        charge.setStatus(paid.signum()==0 ? "DUE" : paid.compareTo(charge.getFinalAmount())>=0 ? "PAID" : "PARTIALLY_PAID");
        charges.save(charge);
    }

    private void ensureGuardianOwnsCharge(Long guardianId, StudentCharge charge) {
        if (charge.getRegistrationCaseId()!=null) {
            var r = registrations.findById(charge.getRegistrationCaseId()).orElseThrow(() -> new FinanceNotFoundException("Dossier d'inscription introuvable."));
            if (!r.getGuardian().getId().equals(guardianId)) throw new FinanceBusinessException("Cet avoir appartient à une autre famille.");
            return;
        }
        if (charge.getStudentEnrollmentId()!=null) {
            var e = enrollments.findById(charge.getStudentEnrollmentId()).orElseThrow(() -> new FinanceNotFoundException("Scolarisation introuvable."));
            if (!guardianLinks.existsByStudentIdAndGuardianIdAndActiveTrue(e.getStudent().getId(), guardianId))
                throw new FinanceBusinessException("Cet avoir appartient à une autre famille.");
            return;
        }
        throw new FinanceBusinessException("Créance sans rattachement financier exploitable.");
    }

    public FamilyCreditResponse toResponse(FamilyCredit x) {
        return new FamilyCreditResponse(x.getId(), x.getGuardianId(), x.getSourcePaymentId(), x.getInitialAmount(), x.getRemainingAmount(), x.getStatus(), x.getCreatedAt());
    }
    private FamilyCreditUsageResponse toUsage(FamilyCreditUsage x){ return new FamilyCreditUsageResponse(x.getId(),x.getFamilyCreditId(),x.getGuardianId(),x.getStudentChargeId(),x.getPaymentAllocationId(),x.getAmount(),x.getCreatedAt()); }
}
