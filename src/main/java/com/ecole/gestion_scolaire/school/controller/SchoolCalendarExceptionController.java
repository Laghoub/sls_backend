package com.ecole.gestion_scolaire.school.controller;

import com.ecole.gestion_scolaire.school.dto.SchoolCalendarExceptionCreateRequest;
import com.ecole.gestion_scolaire.school.dto.SchoolCalendarExceptionResponse;
import com.ecole.gestion_scolaire.school.dto.SchoolCalendarExceptionUpdateRequest;
import com.ecole.gestion_scolaire.school.service.SchoolCalendarExceptionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/school-calendar-exceptions")
public class SchoolCalendarExceptionController {

    private final SchoolCalendarExceptionService service;

    public SchoolCalendarExceptionController(
            SchoolCalendarExceptionService service
    ) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize(
            "hasAuthority('CALENDRIER_SCOLAIRE_CONSULTER')"
    )
    public ResponseEntity<List<SchoolCalendarExceptionResponse>> findAll() {

        return ResponseEntity.ok(
                service.findAll()
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize(
            "hasAuthority('CALENDRIER_SCOLAIRE_CONSULTER')"
    )
    public ResponseEntity<SchoolCalendarExceptionResponse> findById(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                service.findById(id)
        );
    }

    @GetMapping("/school-year/{schoolYearId}")
    @PreAuthorize(
            "hasAuthority('CALENDRIER_SCOLAIRE_CONSULTER')"
    )
    public ResponseEntity<List<SchoolCalendarExceptionResponse>> findBySchoolYear(
            @PathVariable Long schoolYearId
    ) {

        return ResponseEntity.ok(
                service.findBySchoolYear(
                        schoolYearId
                )
        );
    }

    @GetMapping("/school-year/{schoolYearId}/date/{date}")
    @PreAuthorize(
            "hasAuthority('CALENDRIER_SCOLAIRE_CONSULTER')"
    )
    public ResponseEntity<List<SchoolCalendarExceptionResponse>> findByDate(
            @PathVariable Long schoolYearId,
            @PathVariable LocalDate date
    ) {

        return ResponseEntity.ok(
                service.findByDate(
                        schoolYearId,
                        date
                )
        );
    }

    @GetMapping("/school-year/{schoolYearId}/campus/{campusId}")
    @PreAuthorize(
            "hasAuthority('CALENDRIER_SCOLAIRE_CONSULTER')"
    )
    public ResponseEntity<List<SchoolCalendarExceptionResponse>> findByCampus(
            @PathVariable Long schoolYearId,
            @PathVariable Long campusId
    ) {

        return ResponseEntity.ok(
                service.findByCampus(
                        schoolYearId,
                        campusId
                )
        );
    }

    @GetMapping("/school-year/{schoolYearId}/cycle/{cycleId}")
    @PreAuthorize(
            "hasAuthority('CALENDRIER_SCOLAIRE_CONSULTER')"
    )
    public ResponseEntity<List<SchoolCalendarExceptionResponse>> findByCycle(
            @PathVariable Long schoolYearId,
            @PathVariable Long cycleId
    ) {

        return ResponseEntity.ok(
                service.findByCycle(
                        schoolYearId,
                        cycleId
                )
        );
    }

    @PostMapping
    @PreAuthorize(
            "hasAuthority('CALENDRIER_SCOLAIRE_GERER')"
    )
    public ResponseEntity<SchoolCalendarExceptionResponse> create(
            @Valid
            @RequestBody SchoolCalendarExceptionCreateRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        service.create(request)
                );
    }

    @PutMapping("/{id}")
    @PreAuthorize(
            "hasAuthority('CALENDRIER_SCOLAIRE_GERER')"
    )
    public ResponseEntity<SchoolCalendarExceptionResponse> update(
            @PathVariable Long id,
            @Valid
            @RequestBody SchoolCalendarExceptionUpdateRequest request
    ) {

        return ResponseEntity.ok(
                service.update(
                        id,
                        request
                )
        );
    }
}