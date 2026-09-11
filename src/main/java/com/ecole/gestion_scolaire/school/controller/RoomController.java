package com.ecole.gestion_scolaire.school.controller;

import com.ecole.gestion_scolaire.school.dto.RoomCreateRequest;
import com.ecole.gestion_scolaire.school.dto.RoomResponse;
import com.ecole.gestion_scolaire.school.dto.RoomUpdateRequest;
import com.ecole.gestion_scolaire.school.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(
            RoomService roomService
    ) {
        this.roomService = roomService;
    }

    @GetMapping
    @PreAuthorize(
            "hasAuthority('SALLE_CONSULTER')"
    )
    public ResponseEntity<List<RoomResponse>> findAll() {

        return ResponseEntity.ok(
                roomService.findAll()
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize(
            "hasAuthority('SALLE_CONSULTER')"
    )
    public ResponseEntity<RoomResponse> findById(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                roomService.findById(id)
        );
    }

    @GetMapping("/campus/{campusId}")
    @PreAuthorize(
            "hasAuthority('SALLE_CONSULTER')"
    )
    public ResponseEntity<List<RoomResponse>> findByCampus(
            @PathVariable Long campusId
    ) {

        return ResponseEntity.ok(
                roomService.findByCampus(campusId)
        );
    }

    @GetMapping("/campus/{campusId}/active")
    @PreAuthorize(
            "hasAuthority('SALLE_CONSULTER')"
    )
    public ResponseEntity<List<RoomResponse>> findActiveByCampus(
            @PathVariable Long campusId
    ) {

        return ResponseEntity.ok(
                roomService.findActiveByCampus(campusId)
        );
    }

    @PostMapping
    @PreAuthorize(
            "hasAuthority('SALLE_CREER')"
    )
    public ResponseEntity<RoomResponse> create(
            @Valid
            @RequestBody RoomCreateRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        roomService.create(request)
                );
    }

    @PutMapping("/{id}")
    @PreAuthorize(
            "hasAuthority('SALLE_MODIFIER')"
    )
    public ResponseEntity<RoomResponse> update(
            @PathVariable Long id,
            @Valid
            @RequestBody RoomUpdateRequest request
    ) {

        return ResponseEntity.ok(
                roomService.update(
                        id,
                        request
                )
        );
    }
}