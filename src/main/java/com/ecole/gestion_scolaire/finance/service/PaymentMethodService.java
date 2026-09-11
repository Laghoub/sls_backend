package com.ecole.gestion_scolaire.finance.service;

import com.ecole.gestion_scolaire.finance.entity.PaymentMethod;
import com.ecole.gestion_scolaire.finance.exception.*;
import com.ecole.gestion_scolaire.finance.repository.PaymentMethodRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PaymentMethodService {
    private final PaymentMethodRepository repo;

    public PaymentMethodService(PaymentMethodRepository r) {
        repo = r;
    }

    @Transactional(readOnly = true)
    public List<PaymentMethod> all() {
        return repo.findAll();
    }

    @Transactional(readOnly = true)
    public PaymentMethod get(Long id) {
        return repo.findById(id).orElseThrow(() -> new FinanceNotFoundException("Mode de paiement introuvable : " + id));
    }

    @Transactional
    public PaymentMethod save(Long id, String code, String name, boolean active) {
        var x = id == null ? new PaymentMethod() : get(id);
        repo.findByCodeIgnoreCase(code.trim()).filter(o -> id == null || !o.getId().equals(id)).ifPresent(o -> {
            throw new FinanceBusinessException("Code déjà utilisé.");
        });
        x.setCode(code.trim().toUpperCase());
        x.setName(name.trim());
        x.setActive(active);
        return repo.save(x);
    }
}
