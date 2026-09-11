package com.ecole.gestion_scolaire.student.controller;

import com.ecole.gestion_scolaire.common.dto.PageResponse;
import com.ecole.gestion_scolaire.student.dto.*;
import com.ecole.gestion_scolaire.student.service.StudentService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/students")
public class StudentController {
    private final StudentService service;

    public StudentController(StudentService s) {
        service = s;
    }



    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ELEVE_CONSULTER')")
    public StudentResponse one(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ELEVE_CREER')")
    public ResponseEntity<StudentResponse> create(@Valid @RequestBody StudentCreateRequest r) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(r));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ELEVE_MODIFIER')")
    public StudentResponse update(@PathVariable Long id, @Valid @RequestBody StudentUpdateRequest r) {
        return service.update(id, r);
    }

    @PostMapping("/with-person")
    @PreAuthorize("hasAuthority('ELEVE_CREER')")
    public StudentWithPersonResponse createWithPerson(
            @Valid
            @RequestBody
            StudentWithPersonCreateRequest request
    ) {

        return service.createWithPerson(
                request
        );
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ELEVE_CONSULTER')")
    public PageResponse<StudentResponse> search(
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
