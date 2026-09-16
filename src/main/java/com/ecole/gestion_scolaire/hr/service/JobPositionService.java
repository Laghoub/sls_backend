package com.ecole.gestion_scolaire.hr.service;

import com.ecole.gestion_scolaire.common.exception.BusinessRuleException;
import com.ecole.gestion_scolaire.common.exception.ResourceNotFoundException;
import com.ecole.gestion_scolaire.hr.dto.*;
import com.ecole.gestion_scolaire.hr.entity.JobPosition;
import com.ecole.gestion_scolaire.hr.repository.JobPositionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@Transactional
public class JobPositionService {
    private final JobPositionRepository repository;

    public JobPositionService(JobPositionRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<JobPositionResponse> all(boolean includeInactive) {
        var rows = includeInactive
                ? repository.findAllByOrderByDisplayOrderAscNameAsc()
                : repository.findByActiveTrueOrderByDisplayOrderAscNameAsc();
        return rows.stream().map(this::map).toList();
    }

    @Transactional(readOnly = true)
    public JobPositionResponse one(Long id) {
        return map(get(id));
    }

    public JobPositionResponse create(JobPositionRequest request) {
        String code = normalizeCode(request.code());
        String name = request.name().trim();
        ensureUnique(null, code, name);
        var entity = new JobPosition();
        entity.setActive(request.active() == null || request.active());
        apply(entity, request, code, name);
        return map(repository.save(entity));
    }

    public JobPositionResponse update(Long id, JobPositionRequest request) {
        var entity = get(id);
        String code = normalizeCode(request.code());
        String name = request.name().trim();
        ensureUnique(id, code, name);
        apply(entity, request, code, name);
        return map(repository.save(entity));
    }

    public JobPositionResponse changeActive(Long id, boolean active) {
        var entity = get(id);
        entity.setActive(active);
        return map(repository.save(entity));
    }

    public JobPositionResponse archive(Long id) {
        return changeActive(id, false);
    }

    JobPosition get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Poste introuvable"));
    }

    private void ensureUnique(Long currentId, String code, String name) {
        repository.findByCodeIgnoreCase(code).ifPresent(existing -> {
            if (!Objects.equals(existing.getId(), currentId))
                throw new BusinessRuleException("Un poste avec ce code existe déjà.");
        });
        repository.findByNameIgnoreCase(name).ifPresent(existing -> {
            if (!Objects.equals(existing.getId(), currentId))
                throw new BusinessRuleException("Un poste avec cet intitulé existe déjà.");
        });
    }

    private void apply(JobPosition entity, JobPositionRequest request, String code, String name) {
        entity.setCode(code);
        entity.setName(name);
        entity.setCategory(blankToNull(request.category()));
        entity.setDisplayOrder(request.displayOrder());
        if (request.active() != null) entity.setActive(request.active());
    }

    private String normalizeCode(String value) {
        return value.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private JobPositionResponse map(JobPosition entity) {
        return new JobPositionResponse(entity.getId(), entity.getCode(), entity.getName(),
                entity.getCategory(), entity.isActive(), entity.getDisplayOrder());
    }
}
