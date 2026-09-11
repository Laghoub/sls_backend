package com.ecole.gestion_scolaire.finance.service;

import jakarta.persistence.EntityManager;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentFinanceAccountService {
    private final EntityManager em;

    public CurrentFinanceAccountService(EntityManager em) {
        this.em = em;
    }

    public Long id() {
        String u = SecurityContextHolder.getContext().getAuthentication().getName();
        Object x = em.createNativeQuery("select id from user_account where username=:u").setParameter("u", u).getSingleResult();
        return ((Number) x).longValue();
    }
}
