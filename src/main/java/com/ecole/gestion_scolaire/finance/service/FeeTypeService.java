package com.ecole.gestion_scolaire.finance.service;

import com.ecole.gestion_scolaire.common.dto.PageResponse;
import com.ecole.gestion_scolaire.finance.dto.fee.*;
import com.ecole.gestion_scolaire.finance.entity.FeeType;
import com.ecole.gestion_scolaire.finance.exception.*;
import com.ecole.gestion_scolaire.finance.repository.FeeTypeRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeeTypeService {
    private final FeeTypeRepository repo;

    public FeeTypeService(FeeTypeRepository r) {
        repo = r;
    }

    @Transactional(readOnly = true)
    public PageResponse<FeeTypeResponse> search(String q, int page, int size) {
        var p = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)), Sort.by("displayOrder").ascending().and(Sort.by("name")));
        var x = (q == null || q.isBlank()) ? repo.findAll(p) : repo.findByNameContainingIgnoreCaseOrCodeContainingIgnoreCase(q.trim(), q.trim(), p);
        return PageResponse.from(x.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public FeeType get(Long id) {
        return repo.findById(id).orElseThrow(() -> new FinanceNotFoundException("Type de frais introuvable : " + id));
    }

    @Transactional
    public FeeTypeResponse create(FeeTypeRequest r) {
        if (repo.findByCodeIgnoreCase(r.code().trim()).isPresent())
            throw new FinanceBusinessException("Code de frais déjà utilisé.");
        var x = new FeeType();
        apply(x, r);
        return toResponse(repo.save(x));
    }

    @Transactional
    public FeeTypeResponse update(Long id, FeeTypeRequest r) {
        var x = get(id);
        repo.findByCodeIgnoreCase(r.code().trim()).filter(o -> !o.getId().equals(id)).ifPresent(o -> {
            throw new FinanceBusinessException("Code de frais déjà utilisé.");
        });
        apply(x, r);
        return toResponse(repo.save(x));
    }

    private void apply(FeeType x, FeeTypeRequest r) {
        x.setCode(r.code().trim().toUpperCase());
        x.setName(r.name().trim());
        x.setCategory(r.category().trim().toUpperCase());
        x.setActive(r.active());
        x.setDisplayOrder(r.displayOrder());
    }

    public FeeTypeResponse toResponse(FeeType x) {
        return new FeeTypeResponse(x.getId(), x.getCode(), x.getName(), x.getCategory(), x.isActive(), x.getDisplayOrder());
    }
}
