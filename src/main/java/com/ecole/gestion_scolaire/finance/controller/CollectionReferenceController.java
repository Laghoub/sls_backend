package com.ecole.gestion_scolaire.finance.controller;

import com.ecole.gestion_scolaire.finance.dto.collection.GuardianStudentResponse;
import com.ecole.gestion_scolaire.finance.dto.collection.StudentOpenChargeResponse;
import com.ecole.gestion_scolaire.finance.service.CollectionReferenceService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/finance/collection")
public class CollectionReferenceController {
    private final CollectionReferenceService service;
    public CollectionReferenceController(CollectionReferenceService service) { this.service = service; }

    @GetMapping("/guardians/{guardianId}/students")
    @PreAuthorize("hasAuthority('PAIEMENT_CONSULTER')")
    public List<GuardianStudentResponse> students(@PathVariable Long guardianId) {
        return service.students(guardianId);
    }

    @GetMapping("/guardians/{guardianId}/students/{studentId}/open-charges")
    @PreAuthorize("hasAuthority('PAIEMENT_CONSULTER')")
    public List<StudentOpenChargeResponse> openCharges(@PathVariable Long guardianId,
                                                       @PathVariable Long studentId,
                                                       @RequestParam(required = false) Long schoolYearId) {
        return service.openCharges(guardianId, studentId, schoolYearId);
    }
}
