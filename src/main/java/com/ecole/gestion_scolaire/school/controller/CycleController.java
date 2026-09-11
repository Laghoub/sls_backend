package com.ecole.gestion_scolaire.school.controller;

import com.ecole.gestion_scolaire.school.dto.CycleCreateRequest;
import com.ecole.gestion_scolaire.school.dto.CycleResponse;
import com.ecole.gestion_scolaire.school.dto.CycleUpdateRequest;
import com.ecole.gestion_scolaire.school.service.CycleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cycles")
public class CycleController {

    private final CycleService cycleService;

    public CycleController(
            CycleService cycleService
    ) {
        this.cycleService = cycleService;
    }

    @GetMapping
    @PreAuthorize(
            "hasAuthority('CYCLE_CONSULTER')"
    )
    public ResponseEntity<List<CycleResponse>> findAll() {

        return ResponseEntity.ok(
                cycleService.findAll()
        );
    }

    @GetMapping("/active")
    @PreAuthorize(
            "hasAuthority('CYCLE_CONSULTER')"
    )
    public ResponseEntity<List<CycleResponse>> findActive() {

        return ResponseEntity.ok(
                cycleService.findActive()
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize(
            "hasAuthority('CYCLE_CONSULTER')"
    )
    public ResponseEntity<CycleResponse> findById(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                cycleService.findById(id)
        );
    }

    @PostMapping
    @PreAuthorize(
            "hasAuthority('CYCLE_CREER')"
    )
    public ResponseEntity<CycleResponse> create(
            @Valid
            @RequestBody CycleCreateRequest request
    ) {

        CycleResponse response =
                cycleService.create(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize(
            "hasAuthority('CYCLE_MODIFIER')"
    )
    public ResponseEntity<CycleResponse> update(
            @PathVariable Long id,
            @Valid
            @RequestBody CycleUpdateRequest request
    ) {

        return ResponseEntity.ok(
                cycleService.update(
                        id,
                        request
                )
        );
    }
}