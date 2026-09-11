package com.ecole.gestion_scolaire.school.controller;

import com.ecole.gestion_scolaire.school.dto.SchoolYearCreateRequest;
import com.ecole.gestion_scolaire.school.dto.SchoolYearResponse;
import com.ecole.gestion_scolaire.school.dto.SchoolYearUpdateRequest;
import com.ecole.gestion_scolaire.school.service.SchoolYearService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/school-years")
public class SchoolYearController {

    private final SchoolYearService schoolYearService;

    public SchoolYearController(
            SchoolYearService schoolYearService
    ) {
        this.schoolYearService = schoolYearService;
    }

    @GetMapping
    @PreAuthorize(
            "hasAuthority('ANNEE_SCOLAIRE_CONSULTER')"
    )
    public ResponseEntity<List<SchoolYearResponse>> findAll() {

        return ResponseEntity.ok(
                schoolYearService.findAll()
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize(
            "hasAuthority('ANNEE_SCOLAIRE_CONSULTER')"
    )
    public ResponseEntity<SchoolYearResponse> findById(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                schoolYearService.findById(id)
        );
    }

    @GetMapping("/current")
    @PreAuthorize(
            "hasAuthority('ANNEE_SCOLAIRE_CONSULTER')"
    )
    public ResponseEntity<SchoolYearResponse> findCurrent() {

        return ResponseEntity.ok(
                schoolYearService.findCurrent()
        );
    }

    @PostMapping
    @PreAuthorize(
            "hasAuthority('ANNEE_SCOLAIRE_CREER')"
    )
    public ResponseEntity<SchoolYearResponse> create(
            @Valid
            @RequestBody SchoolYearCreateRequest request
    ) {

        SchoolYearResponse response =
                schoolYearService.create(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize(
            "hasAuthority('ANNEE_SCOLAIRE_MODIFIER')"
    )
    public ResponseEntity<SchoolYearResponse> update(
            @PathVariable Long id,
            @Valid
            @RequestBody SchoolYearUpdateRequest request
    ) {

        return ResponseEntity.ok(
                schoolYearService.update(
                        id,
                        request
                )
        );
    }

    @PutMapping("/{id}/current")
    @PreAuthorize(
            "hasAuthority('ANNEE_SCOLAIRE_DEFINIR_COURANTE')"
    )
    public ResponseEntity<SchoolYearResponse> defineAsCurrent(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                schoolYearService.defineAsCurrent(id)
        );
    }
}