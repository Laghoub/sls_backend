package com.ecole.gestion_scolaire.registration.controller;

import com.ecole.gestion_scolaire.common.dto.PageResponse;
import com.ecole.gestion_scolaire.registration.dto.*;
import com.ecole.gestion_scolaire.registration.enums.RegistrationStatus;
import com.ecole.gestion_scolaire.registration.service.*;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/registrations")
public class RegistrationCaseController {
    private final RegistrationCaseService service;
    private final RegistrationDetailService detail;
    private final RegistrationWorkflowService workflow;
    private final RegistrationValidationService validation;

    public RegistrationCaseController(RegistrationCaseService s, RegistrationDetailService d, RegistrationWorkflowService w, RegistrationValidationService v) {
        service = s;
        detail = d;
        workflow = w;
        validation = v;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('INSCRIPTION_CONSULTER')")
    public PageResponse<RegistrationCaseResponse> search(@RequestParam(defaultValue = "") String search, @RequestParam(required = false) Long schoolYearId, @RequestParam(required = false) RegistrationStatus status, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return service.search(search, schoolYearId, status, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('INSCRIPTION_CONSULTER')")
    public RegistrationCaseDetailResponse one(@PathVariable Long id) {
        return detail.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('INSCRIPTION_CREER')")
    public RegistrationCaseResponse create(@Valid @RequestBody RegistrationCaseCreateRequest r) {
        return service.create(r);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('INSCRIPTION_MODIFIER')")
    public RegistrationCaseResponse update(@PathVariable Long id, @Valid @RequestBody RegistrationCaseUpdateRequest r) {
        return service.update(id, r);
    }

    @PostMapping("/{id}/submit-payment")
    @PreAuthorize("hasAuthority('INSCRIPTION_MODIFIER')")
    public RegistrationCaseResponse submit(@PathVariable Long id) {
        return workflow.submitForPayment(id);
    }

    @PostMapping("/{id}/payment-confirmed")
    @PreAuthorize("hasAuthority('INSCRIPTION_PAIEMENT_CONFIRMER')")
    public RegistrationCaseResponse paid(@PathVariable Long id) {
        return workflow.paymentConfirmed(id);
    }

    @PostMapping("/{id}/start-completion")
    @PreAuthorize("hasAuthority('INSCRIPTION_COMPLETER')")
    public RegistrationCaseResponse start(@PathVariable Long id) {
        return workflow.startCompletion(id);
    }

    @PostMapping("/{id}/mark-incomplete")
    @PreAuthorize("hasAuthority('INSCRIPTION_COMPLETER')")
    public RegistrationCaseResponse incomplete(@PathVariable Long id) {
        return workflow.markIncomplete(id);
    }

    @GetMapping("/{id}/validation")
    @PreAuthorize("hasAuthority('INSCRIPTION_CONSULTER')")
    public RegistrationValidationResponse validate(@PathVariable Long id) {
        return validation.validate(id);
    }

    @PostMapping("/{id}/finalize")
    @PreAuthorize("hasAuthority('INSCRIPTION_FINALISER')")
    public RegistrationCaseDetailResponse finalizeCase(@PathVariable Long id, @Valid @RequestBody StudentEnrollmentCreateRequest r) {
        return workflow.finalizeRegistration(id, r);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('INSCRIPTION_ANNULER')")
    public RegistrationCaseResponse cancel(@PathVariable Long id, @Valid @RequestBody CancelRegistrationRequest r) {
        return workflow.cancel(id, r.reason());
    }
}
