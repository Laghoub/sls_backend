package com.ecole.gestion_scolaire.finance.service;

import com.ecole.gestion_scolaire.finance.dto.cash.*;
import com.ecole.gestion_scolaire.finance.entity.CashRegister;
import com.ecole.gestion_scolaire.finance.exception.*;
import com.ecole.gestion_scolaire.finance.repository.CashRegisterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CashRegisterService {
    private final CashRegisterRepository repo;
    private final FinanceReferenceValidator refs;

    public CashRegisterService(CashRegisterRepository r, FinanceReferenceValidator v) {
        repo = r;
        refs = v;
    }

    @Transactional(readOnly = true)
    public List<CashRegisterResponse> all() {
        return repo.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CashRegister get(Long id) {
        return repo.findById(id).orElseThrow(() -> new FinanceNotFoundException("Caisse introuvable : " + id));
    }

    @Transactional
    public CashRegisterResponse save(Long id, CashRegisterRequest r) {
        refs.campus(r.campusId());
        repo.findByCodeIgnoreCase(r.code().trim()).filter(x -> id == null || !x.getId().equals(id)).ifPresent(x -> {
            throw new FinanceBusinessException("Code de caisse déjà utilisé.");
        });
        var x = id == null ? new CashRegister() : get(id);
        x.setCode(r.code().trim().toUpperCase());
        x.setName(r.name().trim());
        x.setCampusId(r.campusId());
        x.setActive(r.active());
        return toResponse(repo.save(x));
    }

    public CashRegisterResponse toResponse(CashRegister x) {
        return new CashRegisterResponse(x.getId(), x.getCode(), x.getName(), x.getCampusId(), x.isActive());
    }
}
