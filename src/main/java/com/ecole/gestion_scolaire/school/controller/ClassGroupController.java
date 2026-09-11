package com.ecole.gestion_scolaire.school.controller;

import com.ecole.gestion_scolaire.school.dto.ClassGroupCreateRequest;
import com.ecole.gestion_scolaire.school.dto.ClassGroupResponse;
import com.ecole.gestion_scolaire.school.dto.ClassGroupUpdateRequest;
import com.ecole.gestion_scolaire.school.service.ClassGroupService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/class-groups")
public class ClassGroupController {

    private final ClassGroupService classGroupService;

    public ClassGroupController(
            ClassGroupService classGroupService
    ) {
        this.classGroupService = classGroupService;
    }

    @GetMapping
    @PreAuthorize(
            "hasAuthority('CLASSE_CONSULTER')"
    )
    public ResponseEntity<List<ClassGroupResponse>> findAll() {

        return ResponseEntity.ok(
                classGroupService.findAll()
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize(
            "hasAuthority('CLASSE_CONSULTER')"
    )
    public ResponseEntity<ClassGroupResponse> findById(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                classGroupService.findById(id)
        );
    }

    @GetMapping("/school-year/{schoolYearId}")
    @PreAuthorize(
            "hasAuthority('CLASSE_CONSULTER')"
    )
    public ResponseEntity<List<ClassGroupResponse>> findBySchoolYear(
            @PathVariable Long schoolYearId
    ) {

        return ResponseEntity.ok(
                classGroupService.findBySchoolYear(
                        schoolYearId
                )
        );
    }

    @GetMapping(
            "/school-year/{schoolYearId}/level/{levelId}"
    )
    @PreAuthorize(
            "hasAuthority('CLASSE_CONSULTER')"
    )
    public ResponseEntity<List<ClassGroupResponse>> findBySchoolYearAndLevel(
            @PathVariable Long schoolYearId,
            @PathVariable Long levelId
    ) {

        return ResponseEntity.ok(
                classGroupService
                        .findBySchoolYearAndLevel(
                                schoolYearId,
                                levelId
                        )
        );
    }

    @GetMapping(
            "/school-year/{schoolYearId}/campus/{campusId}"
    )
    @PreAuthorize(
            "hasAuthority('CLASSE_CONSULTER')"
    )
    public ResponseEntity<List<ClassGroupResponse>> findBySchoolYearAndCampus(
            @PathVariable Long schoolYearId,
            @PathVariable Long campusId
    ) {

        return ResponseEntity.ok(
                classGroupService
                        .findBySchoolYearAndCampus(
                                schoolYearId,
                                campusId
                        )
        );
    }

    @GetMapping(
            "/school-year/{schoolYearId}/level/{levelId}/campus/{campusId}"
    )
    @PreAuthorize(
            "hasAuthority('CLASSE_CONSULTER')"
    )
    public ResponseEntity<List<ClassGroupResponse>> findBySchoolYearLevelAndCampus(
            @PathVariable Long schoolYearId,
            @PathVariable Long levelId,
            @PathVariable Long campusId
    ) {

        return ResponseEntity.ok(
                classGroupService
                        .findBySchoolYearLevelAndCampus(
                                schoolYearId,
                                levelId,
                                campusId
                        )
        );
    }

    @PostMapping
    @PreAuthorize(
            "hasAuthority('CLASSE_CREER')"
    )
    public ResponseEntity<ClassGroupResponse> create(
            @Valid
            @RequestBody ClassGroupCreateRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        classGroupService.create(request)
                );
    }

    @PutMapping("/{id}")
    @PreAuthorize(
            "hasAuthority('CLASSE_MODIFIER')"
    )
    public ResponseEntity<ClassGroupResponse> update(
            @PathVariable Long id,
            @Valid
            @RequestBody ClassGroupUpdateRequest request
    ) {

        return ResponseEntity.ok(
                classGroupService.update(
                        id,
                        request
                )
        );
    }
}