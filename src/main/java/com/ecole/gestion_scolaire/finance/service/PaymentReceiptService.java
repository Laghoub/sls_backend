package com.ecole.gestion_scolaire.finance.service;

import com.ecole.gestion_scolaire.finance.dto.receipt.*;
import com.ecole.gestion_scolaire.finance.exception.FinanceNotFoundException;
import com.ecole.gestion_scolaire.finance.repository.*;
import com.ecole.gestion_scolaire.registration.repository.*;
import com.ecole.gestion_scolaire.student.repository.*;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;

@Service
public class PaymentReceiptService {
    private final PaymentRepository payments;
    private final PaymentMethodRepository methods;
    private final GuardianRepository guardians;
    private final PaymentAllocationRepository allocations;
    private final StudentChargeRepository charges;
    private final StudentEnrollmentRepository enrollments;
    private final RegistrationCaseRepository registrations;
    private final StudentRepository students;
    private final FamilyCreditRepository credits;
    private final FamilyCreditUsageRepository creditUsages;
    private final RefundRepository refunds;
    private final RefundAllocationRepository refundAllocations;

    public PaymentReceiptService(PaymentRepository p, PaymentMethodRepository m, GuardianRepository g,
                                 PaymentAllocationRepository a, StudentChargeRepository c,
                                 StudentEnrollmentRepository e, RegistrationCaseRepository r,
                                 StudentRepository s, FamilyCreditRepository credits,
                                 FamilyCreditUsageRepository creditUsages, RefundRepository refunds,
                                 RefundAllocationRepository refundAllocations) {
        payments=p; methods=m; guardians=g; allocations=a; charges=c; enrollments=e;
        registrations=r; students=s; this.credits=credits; this.creditUsages=creditUsages;
        this.refunds=refunds; this.refundAllocations=refundAllocations;
    }

    @Transactional(readOnly=true)
    public PaymentReceiptResponse get(Long id){
        var p=payments.findById(id).orElseThrow(()->new FinanceNotFoundException("Paiement introuvable."));
        var g=guardians.findById(p.getGuardianId()).orElseThrow(()->new FinanceNotFoundException("Responsable introuvable."));
        var method=methods.findById(p.getPaymentMethodId()).orElseThrow(()->new FinanceNotFoundException("Mode de paiement introuvable."));
        var lines=new ArrayList<PaymentReceiptAllocationResponse>();
        BigDecimal allocatedAmount=BigDecimal.ZERO;

        for(var a:allocations.findByPaymentIdOrderByIdAsc(id)){
            // Une allocation issue de l'utilisation ultérieure d'un avoir ne faisait pas partie du reçu initial.
            if(creditUsages.existsByPaymentAllocationId(a.getId())) continue;
            var c=charges.findById(a.getStudentChargeId()).orElse(null);
            if(c==null)continue;
            Long sid=null;
            if(c.getStudentEnrollmentId()!=null){var e=enrollments.findById(c.getStudentEnrollmentId()).orElse(null);if(e!=null)sid=e.getStudent().getId();}
            if(sid==null&&c.getRegistrationCaseId()!=null){var r=registrations.findById(c.getRegistrationCaseId()).orElse(null);if(r!=null)sid=r.getStudent().getId();}
            var st=sid==null?null:students.findById(sid).orElse(null);
            BigDecimal refunded=refundAllocations.findByPaymentAllocationId(a.getId()).stream()
                    .map(com.ecole.gestion_scolaire.finance.entity.RefundAllocation::getAmount)
                    .reduce(BigDecimal.ZERO,BigDecimal::add);
            BigDecimal net=a.getAmount().subtract(refunded).max(BigDecimal.ZERO);
            allocatedAmount=allocatedAmount.add(a.getAmount());
            lines.add(new PaymentReceiptAllocationResponse(c.getId(),sid,
                    st==null?null:st.getStudentNumber(),
                    st==null?null:st.getPerson().getLastName(),
                    st==null?null:st.getPerson().getFirstName(),
                    c.getLabel(),a.getAmount(),refunded,net));
        }

        BigDecimal creditCreated=credits.findBySourcePaymentIdOrderByCreatedAtAsc(id).stream()
                .map(x->x.getInitialAmount()).reduce(BigDecimal.ZERO,BigDecimal::add);
        BigDecimal creditRemaining=credits.findBySourcePaymentIdOrderByCreatedAtAsc(id).stream()
                .map(x->x.getRemainingAmount()).reduce(BigDecimal.ZERO,BigDecimal::add);
        BigDecimal refunded=refunds.findByPaymentId(id, Pageable.unpaged()).getContent().stream()
                .filter(x->"VALIDATED".equals(x.getStatus()))
                .map(com.ecole.gestion_scolaire.finance.entity.Refund::getAmount)
                .reduce(BigDecimal.ZERO,BigDecimal::add);
        BigDecimal netReceived=p.getTotalAmount().subtract(refunded).max(BigDecimal.ZERO);

        var gp=g.getPerson();
        return new PaymentReceiptResponse(p.getId(),p.getPaymentNumber(),p.getPaymentDate(),p.getTotalAmount(),
                allocatedAmount,creditCreated,creditRemaining,refunded,netReceived,p.getStatus(),g.getId(),
                gp.getLastName(),gp.getFirstName(),gp.getPhone(),gp.getEmail(),method.getId(),method.getCode(),
                method.getName(),p.getExternalReference(),lines);
    }
}
