package com.ecole.gestion_scolaire.school.service;

import com.ecole.gestion_scolaire.common.exception.BusinessRuleException;
import com.ecole.gestion_scolaire.common.exception.ResourceNotFoundException;
import com.ecole.gestion_scolaire.school.dto.CampusCreateRequest;
import com.ecole.gestion_scolaire.school.dto.CampusResponse;
import com.ecole.gestion_scolaire.school.dto.CampusUpdateRequest;
import com.ecole.gestion_scolaire.school.entity.Campus;
import com.ecole.gestion_scolaire.school.repository.CampusRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CampusService {

    private final CampusRepository campusRepository;

    public CampusService(
            CampusRepository campusRepository
    ) {
        this.campusRepository = campusRepository;
    }

    public List<CampusResponse> findAll() {

        return campusRepository
                .findAllByOrderByNameAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<CampusResponse> findActive() {

        return campusRepository
                .findByActiveTrueOrderByNameAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public CampusResponse findById(Long id) {

        return toResponse(
                getEntityById(id)
        );
    }

    @Transactional
    public CampusResponse create(
            CampusCreateRequest request
    ) {

        String code =
                normalizeCode(request.code());

        validateCodeAvailable(code, null);

        Campus campus = new Campus();

        campus.setCode(code);
        campus.setName(normalizeRequired(request.name()));
        campus.setAddress(normalizeNullable(request.address()));
        campus.setPhone(normalizeNullable(request.phone()));
        campus.setActive(request.active());

        return toResponse(
                campusRepository.save(campus)
        );
    }

    @Transactional
    public CampusResponse update(
            Long id,
            CampusUpdateRequest request
    ) {

        Campus campus =
                getEntityById(id);

        String code =
                normalizeCode(request.code());

        validateCodeAvailable(code, id);

        campus.setCode(code);
        campus.setName(normalizeRequired(request.name()));
        campus.setAddress(normalizeNullable(request.address()));
        campus.setPhone(normalizeNullable(request.phone()));
        campus.setActive(request.active());

        return toResponse(
                campusRepository.save(campus)
        );
    }

    private Campus getEntityById(Long id) {

        return campusRepository
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Campus introuvable avec l'identifiant : "
                                        + id
                        )
                );
    }

    private void validateCodeAvailable(
            String code,
            Long currentId
    ) {

        campusRepository
                .findByCode(code)
                .ifPresent(existing -> {

                    if (
                            currentId == null
                                    || !existing.getId().equals(currentId)
                    ) {

                        throw new BusinessRuleException(
                                "Un campus avec le code '"
                                        + code
                                        + "' existe déjà."
                        );
                    }
                });
    }

    private String normalizeCode(String value) {

        return value
                .trim()
                .toUpperCase();
    }

    private String normalizeRequired(String value) {
        return value.trim();
    }

    private String normalizeNullable(String value) {

        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }

    private CampusResponse toResponse(
            Campus campus
    ) {

        return new CampusResponse(
                campus.getId(),
                campus.getCode(),
                campus.getName(),
                campus.getAddress(),
                campus.getPhone(),
                campus.isActive()
        );
    }
}