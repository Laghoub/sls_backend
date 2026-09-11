package com.ecole.gestion_scolaire.school.service;

import com.ecole.gestion_scolaire.common.exception.BusinessRuleException;
import com.ecole.gestion_scolaire.common.exception.ResourceNotFoundException;
import com.ecole.gestion_scolaire.school.dto.LevelCreateRequest;
import com.ecole.gestion_scolaire.school.dto.LevelResponse;
import com.ecole.gestion_scolaire.school.dto.LevelUpdateRequest;
import com.ecole.gestion_scolaire.school.entity.Cycle;
import com.ecole.gestion_scolaire.school.entity.Level;
import com.ecole.gestion_scolaire.school.repository.CycleRepository;
import com.ecole.gestion_scolaire.school.repository.LevelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class LevelService {

    private final LevelRepository levelRepository;
    private final CycleRepository cycleRepository;

    public LevelService(
            LevelRepository levelRepository,
            CycleRepository cycleRepository
    ) {
        this.levelRepository = levelRepository;
        this.cycleRepository = cycleRepository;
    }

    public List<LevelResponse> findAll() {

        return levelRepository
                .findAll()
                .stream()
                .sorted(
                        java.util.Comparator
                                .comparing(
                                        (Level level) ->
                                                level.getCycle().getDisplayOrder()
                                )
                                .thenComparing(Level::getDisplayOrder)
                )
                .map(this::toResponse)
                .toList();
    }

    public List<LevelResponse> findActive() {

        return levelRepository
                .findByActiveTrueOrderByDisplayOrderAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<LevelResponse> findByCycle(
            Long cycleId
    ) {

        ensureCycleExists(cycleId);

        return levelRepository
                .findByCycleIdOrderByDisplayOrderAsc(cycleId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<LevelResponse> findActiveByCycle(
            Long cycleId
    ) {

        ensureCycleExists(cycleId);

        return levelRepository
                .findByCycleIdAndActiveTrueOrderByDisplayOrderAsc(
                        cycleId
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public LevelResponse findById(
            Long id
    ) {

        return toResponse(
                getEntityById(id)
        );
    }

    @Transactional
    public LevelResponse create(
            LevelCreateRequest request
    ) {

        Cycle cycle =
                getCycleById(request.cycleId());

        String normalizedCode =
                normalizeCode(request.code());

        validateCodeAvailable(
                normalizedCode,
                null
        );

        Level level = new Level();

        level.setCycle(cycle);
        level.setCode(normalizedCode);
        level.setName(
                normalizeName(request.name())
        );
        level.setDisplayOrder(
                request.displayOrder()
        );
        level.setActive(
                request.active()
        );

        Level saved =
                levelRepository.save(level);

        return toResponse(saved);
    }

    @Transactional
    public LevelResponse update(
            Long id,
            LevelUpdateRequest request
    ) {

        Level level =
                getEntityById(id);

        Cycle cycle =
                getCycleById(request.cycleId());

        String normalizedCode =
                normalizeCode(request.code());

        validateCodeAvailable(
                normalizedCode,
                id
        );

        level.setCycle(cycle);
        level.setCode(normalizedCode);
        level.setName(
                normalizeName(request.name())
        );
        level.setDisplayOrder(
                request.displayOrder()
        );
        level.setActive(
                request.active()
        );

        Level saved =
                levelRepository.save(level);

        return toResponse(saved);
    }

    private Level getEntityById(
            Long id
    ) {

        return levelRepository
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Niveau introuvable avec l'identifiant : "
                                        + id
                        )
                );
    }

    private Cycle getCycleById(
            Long cycleId
    ) {

        return cycleRepository
                .findById(cycleId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Cycle introuvable avec l'identifiant : "
                                        + cycleId
                        )
                );
    }

    private void ensureCycleExists(
            Long cycleId
    ) {

        if (!cycleRepository.existsById(cycleId)) {

            throw new ResourceNotFoundException(
                    "Cycle introuvable avec l'identifiant : "
                            + cycleId
            );
        }
    }

    private void validateCodeAvailable(
            String code,
            Long currentId
    ) {

        levelRepository
                .findByCode(code)
                .ifPresent(existing -> {

                    if (
                            currentId == null
                                    || !existing.getId().equals(currentId)
                    ) {

                        throw new BusinessRuleException(
                                "Un niveau avec le code '"
                                        + code
                                        + "' existe déjà."
                        );
                    }
                });
    }

    private String normalizeCode(
            String code
    ) {

        return code
                .trim()
                .toUpperCase();
    }

    private String normalizeName(
            String name
    ) {

        return name.trim();
    }

    private LevelResponse toResponse(
            Level level
    ) {

        return new LevelResponse(
                level.getId(),
                level.getCycle().getId(),
                level.getCycle().getCode(),
                level.getCycle().getName(),
                level.getCode(),
                level.getName(),
                level.getDisplayOrder(),
                level.isActive()
        );
    }
}