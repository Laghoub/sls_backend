package com.ecole.gestion_scolaire.student.controller;

import com.ecole.gestion_scolaire.common.dto.PageResponse;
import com.ecole.gestion_scolaire.student.dto.*;
import com.ecole.gestion_scolaire.student.service.GuardianService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/guardians")
public class GuardianController {
    private final GuardianService service;

    public GuardianController(GuardianService s) {
        service = s;
    }



    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('RESPONSABLE_CONSULTER')")
    public GuardianResponse one(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('RESPONSABLE_CREER')")
    public ResponseEntity<GuardianResponse> create(@Valid @RequestBody GuardianCreateRequest r) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(r));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('RESPONSABLE_MODIFIER')")
    public GuardianResponse update(@PathVariable Long id, @Valid @RequestBody GuardianUpdateRequest r) {
        return service.update(id, r);
    }

    @PostMapping("/with-person")
    @PreAuthorize("hasAuthority('RESPONSABLE_CREER')")
    public GuardianWithPersonResponse createWithPerson(
            @Valid
            @RequestBody
            GuardianWithPersonCreateRequest request
    ) {

        return service.createWithPerson(
                request
        );
    }

    @GetMapping
    @PreAuthorize("hasAuthority('RESPONSABLE_CONSULTER')")
    public PageResponse<GuardianResponse> search(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {

        return service.search(
                search,
                page,
                size
        );
    }
}
