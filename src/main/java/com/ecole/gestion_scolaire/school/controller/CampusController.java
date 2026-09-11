package com.ecole.gestion_scolaire.school.controller;

import com.ecole.gestion_scolaire.school.dto.CampusCreateRequest;
import com.ecole.gestion_scolaire.school.dto.CampusResponse;
import com.ecole.gestion_scolaire.school.dto.CampusUpdateRequest;
import com.ecole.gestion_scolaire.school.service.CampusService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/campuses")
public class CampusController {

    private final CampusService campusService;

    public CampusController(
            CampusService campusService
    ) {
        this.campusService = campusService;
    }

    @GetMapping
    @PreAuthorize(
            "hasAuthority('CAMPUS_CONSULTER')"
    )
    public ResponseEntity<List<CampusResponse>> findAll() {

        return ResponseEntity.ok(
                campusService.findAll()
        );
    }

    @GetMapping("/active")
    @PreAuthorize(
            "hasAuthority('CAMPUS_CONSULTER')"
    )
    public ResponseEntity<List<CampusResponse>> findActive() {

        return ResponseEntity.ok(
                campusService.findActive()
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize(
            "hasAuthority('CAMPUS_CONSULTER')"
    )
    public ResponseEntity<CampusResponse> findById(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                campusService.findById(id)
        );
    }

    @PostMapping
    @PreAuthorize(
            "hasAuthority('CAMPUS_CREER')"
    )
    public ResponseEntity<CampusResponse> create(
            @Valid
            @RequestBody CampusCreateRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        campusService.create(request)
                );
    }

    @PutMapping("/{id}")
    @PreAuthorize(
            "hasAuthority('CAMPUS_MODIFIER')"
    )
    public ResponseEntity<CampusResponse> update(
            @PathVariable Long id,
            @Valid
            @RequestBody CampusUpdateRequest request
    ) {

        return ResponseEntity.ok(
                campusService.update(
                        id,
                        request
                )
        );
    }
}