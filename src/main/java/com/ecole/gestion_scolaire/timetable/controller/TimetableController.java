package com.ecole.gestion_scolaire.timetable.controller;

import com.ecole.gestion_scolaire.timetable.dto.*;
import com.ecole.gestion_scolaire.timetable.service.TimetableService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/timetable")
public class TimetableController {
    private final TimetableService service;

    public TimetableController(TimetableService service) { this.service = service; }

    @GetMapping("/class/{classGroupId}")
    @PreAuthorize("hasAuthority('EMPLOI_DU_TEMPS_CONSULTER')")
    public List<ScheduleEntryResponse> byClass(@PathVariable Long classGroupId,
                                               @RequestParam Long schoolYearId) {
        return service.byClass(schoolYearId, classGroupId);
    }

    @GetMapping("/teacher/{teacherId}")
    @PreAuthorize("hasAuthority('EMPLOI_DU_TEMPS_CONSULTER')")
    public List<ScheduleEntryResponse> byTeacher(@PathVariable Long teacherId,
                                                 @RequestParam Long schoolYearId) {
        return service.byTeacher(schoolYearId, teacherId);
    }

    @GetMapping("/eligible-assignments")
    @PreAuthorize("hasAuthority('EMPLOI_DU_TEMPS_CONSULTER')")
    public List<EligibleAssignmentResponse> eligibleAssignments(@RequestParam Long schoolYearId,
                                                                 @RequestParam Long classGroupId) {
        return service.eligibleAssignments(schoolYearId, classGroupId);
    }

    @PostMapping("/entries")
    @PreAuthorize("hasAuthority('EMPLOI_DU_TEMPS_GERER')")
    public ResponseEntity<ScheduleEntryResponse> create(@Valid @RequestBody ScheduleEntryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/entries/{id}")
    @PreAuthorize("hasAuthority('EMPLOI_DU_TEMPS_GERER')")
    public ScheduleEntryResponse update(@PathVariable Long id,
                                        @Valid @RequestBody ScheduleEntryRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/entries/{id}")
    @PreAuthorize("hasAuthority('EMPLOI_DU_TEMPS_GERER')")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        service.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/class/{classGroupId}/grid")
    @PreAuthorize("hasAuthority('EMPLOI_DU_TEMPS_GERER')")
    public List<ScheduleEntryResponse> replaceGrid(@PathVariable Long classGroupId,
                                                   @Valid @RequestBody TimetableGridSaveRequest request) {
        if (!classGroupId.equals(request.classGroupId())) {
            throw new IllegalArgumentException("La classe de l'URL ne correspond pas à la classe du contenu.");
        }
        return service.replaceClassGrid(request);
    }
}
