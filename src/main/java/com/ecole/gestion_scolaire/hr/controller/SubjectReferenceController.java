package com.ecole.gestion_scolaire.hr.controller;

import com.ecole.gestion_scolaire.hr.dto.*;
import com.ecole.gestion_scolaire.hr.service.SubjectReferenceService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/hr/subjects")
public class SubjectReferenceController {
    private final SubjectReferenceService service;

    public SubjectReferenceController(SubjectReferenceService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('MATIERE_CONSULTER') or hasAuthority('ENSEIGNANT_CONSULTER')")
    public List<SubjectResponse> all(@RequestParam(defaultValue = "false") boolean includeInactive) {
        return service.all(includeInactive);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('MATIERE_CONSULTER') or hasAuthority('ENSEIGNANT_CONSULTER')")
    public SubjectResponse one(@PathVariable Long id) {
        return service.one(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('MATIERE_GERER')")
    public ResponseEntity<SubjectResponse> create(@Valid @RequestBody SubjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('MATIERE_GERER')")
    public SubjectResponse update(@PathVariable Long id, @Valid @RequestBody SubjectRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/active/{active}")
    @PreAuthorize("hasAuthority('MATIERE_GERER')")
    public SubjectResponse active(@PathVariable Long id, @PathVariable boolean active) {
        return service.changeActive(id, active);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MATIERE_GERER')")
    public SubjectResponse archive(@PathVariable Long id) {
        return service.archive(id);
    }
}
