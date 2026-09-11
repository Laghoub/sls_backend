package com.ecole.gestion_scolaire.finance.controller;

import com.ecole.gestion_scolaire.common.dto.PageResponse;
import com.ecole.gestion_scolaire.finance.dto.charge.*;
import com.ecole.gestion_scolaire.finance.service.StudentChargeService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/finance/charges")
public class StudentChargeController {
    private final StudentChargeService s;

    public StudentChargeController(StudentChargeService x) {
        s = x;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CREANCE_CONSULTER')")
    public PageResponse<StudentChargeResponse> all(@RequestParam(required = false) Long registrationCaseId, @RequestParam(required = false) Long studentEnrollmentId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return s.search(registrationCaseId, studentEnrollmentId, page, size);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CREANCE_GERER')")
    public StudentChargeResponse create(@Valid @RequestBody StudentChargeCreateRequest r) {
        return s.create(r);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('CREANCE_GERER')")
    public StudentChargeResponse cancel(@PathVariable Long id, @RequestBody java.util.Map<String, String> b) {
        return s.cancel(id, b.getOrDefault("reason", "Annulation"));
    }
}
