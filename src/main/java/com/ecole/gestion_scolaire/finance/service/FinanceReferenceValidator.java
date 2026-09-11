package com.ecole.gestion_scolaire.finance.service;

import com.ecole.gestion_scolaire.finance.exception.*;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;

@Service
public class FinanceReferenceValidator {
    private final EntityManager em;

    public FinanceReferenceValidator(EntityManager em) {
        this.em = em;
    }

    public void guardian(Long id) {
        exists("guardian", id, "Responsable");
    }

    public void schoolYear(Long id) {
        exists("school_year", id, "Année scolaire");
    }

    public void level(Long id) {
        if (id != null) exists("level", id, "Niveau");
    }

    public void classGroup(Long id) {
        if (id != null) exists("class_group", id, "Classe");
    }

    public void campus(Long id) {
        if (id != null) exists("campus", id, "Campus");
    }

    public void registration(Long id) {
        if (id != null) exists("registration_case", id, "Dossier d'inscription");
    }

    public void enrollment(Long id) {
        if (id != null) exists("student_enrollment", id, "Scolarisation");
    }

    private void exists(String table, Long id, String label) {
        if (id == null) throw new FinanceBusinessException(label + " obligatoire.");
        Number n = (Number) em.createNativeQuery("select count(*) from " + table + " where id=:id").setParameter("id", id).getSingleResult();
        if (n.longValue() == 0) throw new FinanceNotFoundException(label + " introuvable : " + id);
    }
}
