package com.ecole.gestion_scolaire.school.controller;

import com.ecole.gestion_scolaire.school.dto.LevelCreateRequest;
import com.ecole.gestion_scolaire.school.dto.LevelResponse;
import com.ecole.gestion_scolaire.school.dto.LevelUpdateRequest;
import com.ecole.gestion_scolaire.school.service.LevelService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/levels")
public class LevelController {

    private final LevelService levelService;

    public LevelController(
            LevelService levelService
    ) {
        this.levelService = levelService;
    }

    @GetMapping
    @PreAuthorize(
            "hasAuthority('NIVEAU_CONSULTER')"
    )
    public ResponseEntity<List<LevelResponse>> findAll() {

        return ResponseEntity.ok(
                levelService.findAll()
        );
    }

    @GetMapping("/active")
    @PreAuthorize(
            "hasAuthority('NIVEAU_CONSULTER')"
    )
    public ResponseEntity<List<LevelResponse>> findActive() {

        return ResponseEntity.ok(
                levelService.findActive()
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize(
            "hasAuthority('NIVEAU_CONSULTER')"
    )
    public ResponseEntity<LevelResponse> findById(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                levelService.findById(id)
        );
    }

    @GetMapping("/cycle/{cycleId}")
    @PreAuthorize(
            "hasAuthority('NIVEAU_CONSULTER')"
    )
    public ResponseEntity<List<LevelResponse>> findByCycle(
            @PathVariable Long cycleId
    ) {

        return ResponseEntity.ok(
                levelService.findByCycle(cycleId)
        );
    }

    @GetMapping("/cycle/{cycleId}/active")
    @PreAuthorize(
            "hasAuthority('NIVEAU_CONSULTER')"
    )
    public ResponseEntity<List<LevelResponse>> findActiveByCycle(
            @PathVariable Long cycleId
    ) {

        return ResponseEntity.ok(
                levelService.findActiveByCycle(cycleId)
        );
    }

    @PostMapping
    @PreAuthorize(
            "hasAuthority('NIVEAU_CREER')"
    )
    public ResponseEntity<LevelResponse> create(
            @Valid
            @RequestBody LevelCreateRequest request
    ) {

        LevelResponse response =
                levelService.create(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize(
            "hasAuthority('NIVEAU_MODIFIER')"
    )
    public ResponseEntity<LevelResponse> update(
            @PathVariable Long id,
            @Valid
            @RequestBody LevelUpdateRequest request
    ) {

        return ResponseEntity.ok(
                levelService.update(
                        id,
                        request
                )
        );
    }
}