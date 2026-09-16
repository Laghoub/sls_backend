package com.ecole.gestion_scolaire.hr.controller;

import com.ecole.gestion_scolaire.hr.dto.*;
import com.ecole.gestion_scolaire.hr.service.JobPositionService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/hr/job-positions")
public class JobPositionController {
    private final JobPositionService service;

    public JobPositionController(JobPositionService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('POSTE_CONSULTER') or hasAuthority('PERSONNEL_CONSULTER')")
    public List<JobPositionResponse> all(@RequestParam(defaultValue = "false") boolean includeInactive) {
        return service.all(includeInactive);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('POSTE_CONSULTER') or hasAuthority('PERSONNEL_CONSULTER')")
    public JobPositionResponse one(@PathVariable Long id) {
        return service.one(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('POSTE_GERER')")
    public ResponseEntity<JobPositionResponse> create(@Valid @RequestBody JobPositionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('POSTE_GERER')")
    public JobPositionResponse update(@PathVariable Long id, @Valid @RequestBody JobPositionRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/active/{active}")
    @PreAuthorize("hasAuthority('POSTE_GERER')")
    public JobPositionResponse active(@PathVariable Long id, @PathVariable boolean active) {
        return service.changeActive(id, active);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('POSTE_GERER')")
    public JobPositionResponse archive(@PathVariable Long id) {
        return service.archive(id);
    }
}
