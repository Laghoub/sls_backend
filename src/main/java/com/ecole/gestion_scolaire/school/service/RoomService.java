package com.ecole.gestion_scolaire.school.service;

import com.ecole.gestion_scolaire.common.exception.BusinessRuleException;
import com.ecole.gestion_scolaire.common.exception.ResourceNotFoundException;
import com.ecole.gestion_scolaire.school.dto.RoomCreateRequest;
import com.ecole.gestion_scolaire.school.dto.RoomResponse;
import com.ecole.gestion_scolaire.school.dto.RoomUpdateRequest;
import com.ecole.gestion_scolaire.school.entity.Campus;
import com.ecole.gestion_scolaire.school.entity.Room;
import com.ecole.gestion_scolaire.school.repository.CampusRepository;
import com.ecole.gestion_scolaire.school.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class RoomService {

    private final RoomRepository roomRepository;
    private final CampusRepository campusRepository;

    public RoomService(
            RoomRepository roomRepository,
            CampusRepository campusRepository
    ) {
        this.roomRepository = roomRepository;
        this.campusRepository = campusRepository;
    }

    public RoomResponse findById(Long id) {

        return toResponse(
                getEntityById(id)
        );
    }

    public List<RoomResponse> findAll() {

        return roomRepository
                .findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<RoomResponse> findByCampus(
            Long campusId
    ) {

        ensureCampusExists(campusId);

        return roomRepository
                .findByCampusIdOrderByNameAsc(campusId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<RoomResponse> findActiveByCampus(
            Long campusId
    ) {

        ensureCampusExists(campusId);

        return roomRepository
                .findByCampusIdAndActiveTrueOrderByNameAsc(
                        campusId
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public RoomResponse create(
            RoomCreateRequest request
    ) {

        Campus campus =
                getCampusById(request.campusId());

        String code =
                normalizeCode(request.code());

        validateCodeAvailable(
                campus.getId(),
                code,
                null
        );

        Room room = new Room();

        room.setCampus(campus);
        room.setCode(code);
        room.setName(
                normalizeRequired(request.name())
        );
        room.setCapacity(
                request.capacity()
        );
        room.setRoomType(
                normalizeNullableUppercase(
                        request.roomType()
                )
        );
        room.setActive(
                request.active()
        );

        return toResponse(
                roomRepository.save(room)
        );
    }

    @Transactional
    public RoomResponse update(
            Long id,
            RoomUpdateRequest request
    ) {

        Room room =
                getEntityById(id);

        Campus campus =
                getCampusById(request.campusId());

        String code =
                normalizeCode(request.code());

        validateCodeAvailable(
                campus.getId(),
                code,
                id
        );

        room.setCampus(campus);
        room.setCode(code);
        room.setName(
                normalizeRequired(request.name())
        );
        room.setCapacity(
                request.capacity()
        );
        room.setRoomType(
                normalizeNullableUppercase(
                        request.roomType()
                )
        );
        room.setActive(
                request.active()
        );

        return toResponse(
                roomRepository.save(room)
        );
    }

    private Room getEntityById(
            Long id
    ) {

        return roomRepository
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Salle introuvable avec l'identifiant : "
                                        + id
                        )
                );
    }

    private Campus getCampusById(
            Long campusId
    ) {

        return campusRepository
                .findById(campusId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Campus introuvable avec l'identifiant : "
                                        + campusId
                        )
                );
    }

    private void ensureCampusExists(
            Long campusId
    ) {

        if (!campusRepository.existsById(campusId)) {

            throw new ResourceNotFoundException(
                    "Campus introuvable avec l'identifiant : "
                            + campusId
            );
        }
    }

    private void validateCodeAvailable(
            Long campusId,
            String code,
            Long currentId
    ) {

        roomRepository
                .findByCampusIdAndCode(
                        campusId,
                        code
                )
                .ifPresent(existing -> {

                    if (
                            currentId == null
                                    || !existing.getId().equals(currentId)
                    ) {

                        throw new BusinessRuleException(
                                "Une salle avec le code '"
                                        + code
                                        + "' existe déjà dans ce campus."
                        );
                    }
                });
    }

    private String normalizeCode(
            String value
    ) {

        return value
                .trim()
                .toUpperCase();
    }

    private String normalizeRequired(
            String value
    ) {

        return value.trim();
    }

    private String normalizeNullableUppercase(
            String value
    ) {

        if (value == null) {
            return null;
        }

        String normalized =
                value.trim();

        if (normalized.isEmpty()) {
            return null;
        }

        return normalized.toUpperCase();
    }

    private RoomResponse toResponse(
            Room room
    ) {

        return new RoomResponse(
                room.getId(),
                room.getCampus().getId(),
                room.getCampus().getCode(),
                room.getCampus().getName(),
                room.getCode(),
                room.getName(),
                room.getCapacity(),
                room.getRoomType(),
                room.isActive()
        );
    }
}