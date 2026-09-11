package com.ecole.gestion_scolaire.school.service;

import com.ecole.gestion_scolaire.common.exception.BusinessRuleException;
import com.ecole.gestion_scolaire.common.exception.ResourceNotFoundException;
import com.ecole.gestion_scolaire.school.dto.CycleCreateRequest;
import com.ecole.gestion_scolaire.school.dto.CycleResponse;
import com.ecole.gestion_scolaire.school.dto.CycleUpdateRequest;
import com.ecole.gestion_scolaire.school.entity.Cycle;
import com.ecole.gestion_scolaire.school.repository.CycleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CycleService {

    private final CycleRepository cycleRepository;

    public CycleService(
            CycleRepository cycleRepository
    ) {
        this.cycleRepository = cycleRepository;
    }

    public List<CycleResponse> findAll() {

        return cycleRepository
                .findAllByOrderByDisplayOrderAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<CycleResponse> findActive() {

        return cycleRepository
                .findByActiveTrueOrderByDisplayOrderAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public CycleResponse findById(Long id) {

        return toResponse(
                getEntityById(id)
        );
    }

    @Transactional
    public CycleResponse create(
            CycleCreateRequest request
    ) {

        String normalizedCode =
                normalizeCode(request.code());

        validateCodeAvailable(
                normalizedCode,
                null
        );

        Cycle cycle = new Cycle();

        cycle.setCode(normalizedCode);
        cycle.setName(normalizeName(request.name()));
        cycle.setDisplayOrder(request.displayOrder());
        cycle.setActive(request.active());

        Cycle saved =
                cycleRepository.save(cycle);

        return toResponse(saved);
    }

    @Transactional
    public CycleResponse update(
            Long id,
            CycleUpdateRequest request
    ) {

        Cycle cycle =
                getEntityById(id);

        String normalizedCode =
                normalizeCode(request.code());

        validateCodeAvailable(
                normalizedCode,
                id
        );

        cycle.setCode(normalizedCode);
        cycle.setName(normalizeName(request.name()));
        cycle.setDisplayOrder(request.displayOrder());
        cycle.setActive(request.active());

        Cycle saved =
                cycleRepository.save(cycle);

        return toResponse(saved);
    }

    private Cycle getEntityById(Long id) {

        return cycleRepository
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Cycle introuvable avec l'identifiant : "
                                        + id
                        )
                );
    }

    private void validateCodeAvailable(
            String code,
            Long currentId
    ) {

        cycleRepository
                .findByCode(code)
                .ifPresent(existing -> {

                    if (
                            currentId == null
                                    || !existing.getId().equals(currentId)
                    ) {

                        throw new BusinessRuleException(
                                "Un cycle avec le code '"
                                        + code
                                        + "' existe déjà."
                        );
                    }
                });
    }

    private String normalizeCode(String code) {

        return code
                .trim()
                .toUpperCase();
    }

    private String normalizeName(String name) {

        return name.trim();
    }

    private CycleResponse toResponse(
            Cycle cycle
    ) {

        return new CycleResponse(
                cycle.getId(),
                cycle.getCode(),
                cycle.getName(),
                cycle.getDisplayOrder(),
                cycle.isActive()
        );
    }
}