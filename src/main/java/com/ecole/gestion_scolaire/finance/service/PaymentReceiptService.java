package com.ecole.gestion_scolaire.finance.service;

import com.ecole.gestion_scolaire.finance.dto.receipt.*;
import com.ecole.gestion_scolaire.finance.exception.FinanceNotFoundException;
import com.ecole.gestion_scolaire.finance.repository.*;
import com.ecole.gestion_scolaire.registration.repository.*;
import com.ecole.gestion_scolaire.student.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

@Service
public class PaymentReceiptService {
    private final PaymentRepository payments; private final PaymentMethodRepository methods; private final GuardianRepository guardians;
    private final PaymentAllocationRepository allocations; private final StudentChargeRepository charges;
    private final StudentEnrollmentRepository enrollments; private final RegistrationCaseRepository registrations; private final StudentRepository students;
    public PaymentReceiptService(PaymentRepository p, PaymentMethodRepository m, GuardianRepository g, PaymentAllocationRepository a,
                                 StudentChargeRepository c, StudentEnrollmentRepository e, RegistrationCaseRepository r, StudentRepository s){payments=p;methods=m;guardians=g;allocations=a;charges=c;enrollments=e;registrations=r;students=s;}
    @Transactional(readOnly=true)
    public PaymentReceiptResponse get(Long id){
        var p=payments.findById(id).orElseThrow(()->new FinanceNotFoundException("Paiement introuvable."));
        var g=guardians.findById(p.getGuardianId()).orElseThrow(()->new FinanceNotFoundException("Responsable introuvable."));
        var method=methods.findById(p.getPaymentMethodId()).orElseThrow(()->new FinanceNotFoundException("Mode de paiement introuvable."));
        var lines=new ArrayList<PaymentReceiptAllocationResponse>();
        for(var a:allocations.findByPaymentIdOrderByIdAsc(id)){
            var c=charges.findById(a.getStudentChargeId()).orElse(null); if(c==null)continue; Long sid=null;
            if(c.getStudentEnrollmentId()!=null){var e=enrollments.findById(c.getStudentEnrollmentId()).orElse(null);if(e!=null)sid=e.getStudent().getId();}
            if(sid==null&&c.getRegistrationCaseId()!=null){var r=registrations.findById(c.getRegistrationCaseId()).orElse(null);if(r!=null)sid=r.getStudent().getId();}
            var st=sid==null?null:students.findById(sid).orElse(null);
            lines.add(new PaymentReceiptAllocationResponse(c.getId(),sid,st==null?null:st.getStudentNumber(),st==null?null:st.getPerson().getLastName(),st==null?null:st.getPerson().getFirstName(),c.getLabel(),a.getAmount()));
        }
        var gp=g.getPerson();
        return new PaymentReceiptResponse(p.getId(),p.getPaymentNumber(),p.getPaymentDate(),p.getTotalAmount(),p.getStatus(),g.getId(),gp.getLastName(),gp.getFirstName(),gp.getPhone(),gp.getEmail(),method.getId(),method.getCode(),method.getName(),p.getExternalReference(),lines);
    }
}
