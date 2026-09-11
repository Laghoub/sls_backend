package com.ecole.gestion_scolaire.finance.integration;

import com.ecole.gestion_scolaire.finance.repository.StudentChargeRepository;
import com.ecole.gestion_scolaire.registration.enums.RegistrationStatus;
import com.ecole.gestion_scolaire.registration.service.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationPaymentService {
    private final RegistrationCaseService cases;
    private final RegistrationWorkflowService workflow;
    private final StudentChargeRepository charges;

    public RegistrationPaymentService(RegistrationCaseService c, RegistrationWorkflowService w, StudentChargeRepository s) {
        cases = c;
        workflow = w;
        charges = s;
    }

    @Transactional
    public void refresh(Long registrationId) {
        if (registrationId == null) return;
        var c = cases.get(registrationId);
        if (c.getStatus() != RegistrationStatus.EN_ATTENTE_PAIEMENT) return;
        var unpaid = charges.findByRegistrationCaseIdAndStatusNot(registrationId, "CANCELLED").stream().anyMatch(x -> !"PAID".equals(x.getStatus()));
        if (!unpaid) workflow.paymentConfirmed(registrationId);
    }
}
