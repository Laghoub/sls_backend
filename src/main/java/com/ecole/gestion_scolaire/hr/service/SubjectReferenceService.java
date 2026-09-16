package com.ecole.gestion_scolaire.hr.service;

import com.ecole.gestion_scolaire.common.exception.BusinessRuleException;
import com.ecole.gestion_scolaire.common.exception.ResourceNotFoundException;
import com.ecole.gestion_scolaire.hr.dto.*;
import com.ecole.gestion_scolaire.hr.entity.Subject;
import com.ecole.gestion_scolaire.hr.repository.SubjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@Transactional
public class SubjectReferenceService {
    private final SubjectRepository repository;

    public SubjectReferenceService(SubjectRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<SubjectResponse> all(boolean includeInactive) {
        var rows = includeInactive
                ? repository.findAllByOrderByDisplayOrderAscNameAsc()
                : repository.findByActiveTrueOrderByDisplayOrderAscNameAsc();
        return rows.stream().map(this::map).toList();
    }

    @Transactional(readOnly = true)
    public SubjectResponse one(Long id) {
        return map(get(id));
    }

    public SubjectResponse create(SubjectRequest request) {
        String code = normalizeCode(request.code());
        String name = request.name().trim();
        ensureUnique(null, code, name);
        var entity = new Subject();
        entity.setActive(request.active() == null || request.active());
        apply(entity, request, code, name);
        return map(repository.save(entity));
    }

    public SubjectResponse update(Long id, SubjectRequest request) {
        var entity = get(id);
        String code = normalizeCode(request.code());
        String name = request.name().trim();
        ensureUnique(id, code, name);
        apply(entity, request, code, name);
        return map(repository.save(entity));
    }

    public SubjectResponse changeActive(Long id, boolean active) {
        var entity = get(id);
        entity.setActive(active);
        return map(repository.save(entity));
    }

    // On ne supprime pas physiquement une matière : elle peut être référencée par des
    // affectations, tarifs, résultats ou historiques. DELETE signifie donc archivage.
    public SubjectResponse archive(Long id) {
        return changeActive(id, false);
    }

    private Subject get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Matière introuvable"));
    }

    private void ensureUnique(Long currentId, String code, String name) {
        repository.findByCodeIgnoreCase(code).ifPresent(existing -> {
            if (!Objects.equals(existing.getId(), currentId))
                throw new BusinessRuleException("Une matière avec ce code existe déjà.");
        });
        repository.findByNameIgnoreCase(name).ifPresent(existing -> {
            if (!Objects.equals(existing.getId(), currentId))
                throw new BusinessRuleException("Une matière avec ce nom existe déjà.");
        });
    }

    private void apply(Subject entity, SubjectRequest request, String code, String name) {
        entity.setCode(code);
        entity.setName(name);
        entity.setDisplayOrder(request.displayOrder());
        if (request.active() != null) entity.setActive(request.active());
    }

    private String normalizeCode(String value) {
        return value.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }

    private SubjectResponse map(Subject entity) {
        return new SubjectResponse(entity.getId(), entity.getCode(), entity.getName(),
                entity.isActive(), entity.getDisplayOrder());
    }
}
