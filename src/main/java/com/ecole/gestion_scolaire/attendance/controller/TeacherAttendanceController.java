package com.ecole.gestion_scolaire.attendance.controller;

import com.ecole.gestion_scolaire.attendance.dto.*;
import com.ecole.gestion_scolaire.attendance.enums.AttendanceValidationStatus;
import com.ecole.gestion_scolaire.attendance.enums.TeacherAttendanceStatus;
import com.ecole.gestion_scolaire.attendance.service.TeacherAttendanceService;
import com.ecole.gestion_scolaire.common.dto.PageResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/teacher-attendance")
public class TeacherAttendanceController {
    private final TeacherAttendanceService service;

    public TeacherAttendanceController(TeacherAttendanceService service) {
        this.service = service;
    }

    @GetMapping("/expected")
    @PreAuthorize("hasAuthority('ASSIDUITE_ENSEIGNANT_CONSULTER')")
    public List<ExpectedTeacherAttendanceResponse> expected(@RequestParam LocalDate date,
                                                            @RequestParam Long timeSlotId) {
        return service.expected(date, timeSlotId);
    }

    @PutMapping("/batch")
    @PreAuthorize("hasAuthority('ASSIDUITE_ENSEIGNANT_SAISIR')")
    public List<TeacherAttendanceResponse> saveBatch(@Valid @RequestBody TeacherAttendanceBatchRequest request) {
        return service.saveBatch(request);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ASSIDUITE_ENSEIGNANT_CONSULTER')")
    public PageResponse<TeacherAttendanceResponse> history(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) TeacherAttendanceStatus status,
            @RequestParam(required = false) AttendanceValidationStatus validationStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.history(from, to, teacherId, status, validationStatus, page, size);
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('ASSIDUITE_ENSEIGNANT_CONSULTER')")
    public TeacherAttendanceSummaryResponse summary(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) Long teacherId) {
        return service.summary(from, to, teacherId);
    }

    @PatchMapping("/{id}/validate")
    @PreAuthorize("hasAuthority('ASSIDUITE_ENSEIGNANT_VALIDER')")
    public TeacherAttendanceResponse validate(@PathVariable Long id) {
        return service.validate(id);
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('ASSIDUITE_ENSEIGNANT_VALIDER')")
    public TeacherAttendanceResponse reject(@PathVariable Long id) {
        return service.reject(id);
    }

    @PatchMapping("/{id}/reopen")
    @PreAuthorize("hasAuthority('ASSIDUITE_ENSEIGNANT_VALIDER')")
    public TeacherAttendanceResponse reopen(@PathVariable Long id) {
        return service.reopen(id);
    }
}
