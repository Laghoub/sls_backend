package com.ecole.gestion_scolaire.student.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;

@Component
public class StudentNumberGenerator {

    @PersistenceContext
    private EntityManager entityManager;


    @Transactional
    public String generate() {

        Number sequenceValue =
                (Number) entityManager
                        .createNativeQuery(
                                "SELECT nextval('student_number_seq')"
                        )
                        .getSingleResult();

        long number =
                sequenceValue.longValue();

        int year =
                Year.now().getValue();

        return String.format(
                "ELV-%d-%06d",
                year,
                number
        );
    }
}