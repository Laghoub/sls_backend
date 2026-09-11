package com.ecole.gestion_scolaire.registration.service;

import com.ecole.gestion_scolaire.registration.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationDetailService {
    private final RegistrationCaseService cases;
    private final RegistrationInfoService info;
    private final RegistrationConsentService consents;
    private final RegistrationDocumentService docs;
    private final RegistrationValidationService validation;
    private final StudentEnrollmentService enrollments;

    public RegistrationDetailService(RegistrationCaseService c, RegistrationInfoService i, RegistrationConsentService co, RegistrationDocumentService d, RegistrationValidationService v, StudentEnrollmentService e) {
        cases = c;
        info = i;
        consents = co;
        docs = d;
        validation = v;
        enrollments = e;
    }
    @Transactional(readOnly = true)
    public RegistrationCaseDetailResponse get(Long id) {
        var c = cases.get(id);
        return new RegistrationCaseDetailResponse(cases.toResponse(c), info.find(id), consents.list(id), docs.list(id), validation.validate(id), enrollments.findByStudentAndYear(c.getStudent().getId(), c.getSchoolYearId()));
    }
}
