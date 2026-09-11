package com.ecole.gestion_scolaire.finance.service;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;

import java.time.Year;

@Service
public class PaymentNumberGenerator {
    private final EntityManager em;

    public PaymentNumberGenerator(EntityManager e) {
        em = e;
    }

    public String next() {
        Number n = (Number) em.createNativeQuery("select nextval('payment_number_seq')").getSingleResult();
        return "PAY-%d-%06d".formatted(Year.now().getValue(), n.longValue());
    }
}
