package com.ecole.gestion_scolaire.school.controller;

import com.ecole.gestion_scolaire.school.dto.TimeSlotCreateRequest;
import com.ecole.gestion_scolaire.school.dto.TimeSlotResponse;
import com.ecole.gestion_scolaire.school.dto.TimeSlotUpdateRequest;
import com.ecole.gestion_scolaire.school.service.TimeSlotService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/time-slots")
public class TimeSlotController {

    private final TimeSlotService timeSlotService;

    public TimeSlotController(
            TimeSlotService timeSlotService
    ) {
        this.timeSlotService = timeSlotService;
    }

    @GetMapping
    @PreAuthorize(
            "hasAuthority('CRENEAU_CONSULTER')"
    )
    public ResponseEntity<List<TimeSlotResponse>> findAll() {

        return ResponseEntity.ok(
                timeSlotService.findAll()
        );
    }

    @GetMapping("/active")
    @PreAuthorize(
            "hasAuthority('CRENEAU_CONSULTER')"
    )
    public ResponseEntity<List<TimeSlotResponse>> findActive() {

        return ResponseEntity.ok(
                timeSlotService.findActive()
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize(
            "hasAuthority('CRENEAU_CONSULTER')"
    )
    public ResponseEntity<TimeSlotResponse> findById(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                timeSlotService.findById(id)
        );
    }

    @PostMapping
    @PreAuthorize(
            "hasAuthority('CRENEAU_CREER')"
    )
    public ResponseEntity<TimeSlotResponse> create(
            @Valid
            @RequestBody TimeSlotCreateRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        timeSlotService.create(request)
                );
    }

    @PutMapping("/{id}")
    @PreAuthorize(
            "hasAuthority('CRENEAU_MODIFIER')"
    )
    public ResponseEntity<TimeSlotResponse> update(
            @PathVariable Long id,
            @Valid
            @RequestBody TimeSlotUpdateRequest request
    ) {

        return ResponseEntity.ok(
                timeSlotService.update(
                        id,
                        request
                )
        );
    }
}