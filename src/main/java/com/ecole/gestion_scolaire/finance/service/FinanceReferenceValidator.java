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

    public void cycle(Long id) {
        if (id != null) exists("cycle", id, "Cycle");
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


    public void tariffScope(Long schoolYearId, Long cycleId, Long levelId, Long classGroupId, Long campusId) {
        if (levelId != null && cycleId != null) {
            Number n = (Number) em.createNativeQuery("select count(*) from level where id=:level and cycle_id=:cycle")
                    .setParameter("level", levelId).setParameter("cycle", cycleId).getSingleResult();
            if (n.longValue() == 0) throw new FinanceBusinessException("Le niveau sélectionné n'appartient pas au cycle du tarif.");
        }
        if (classGroupId != null) {
            StringBuilder sql = new StringBuilder("select count(*) from class_group cg join level l on l.id=cg.level_id where cg.id=:classId and cg.school_year_id=:year");
            if (levelId != null) sql.append(" and cg.level_id=:levelId");
            if (cycleId != null) sql.append(" and l.cycle_id=:cycleId");
            if (campusId != null) sql.append(" and cg.campus_id=:campusId");
            var q = em.createNativeQuery(sql.toString()).setParameter("classId", classGroupId).setParameter("year", schoolYearId);
            if (levelId != null) q.setParameter("levelId", levelId);
            if (cycleId != null) q.setParameter("cycleId", cycleId);
            if (campusId != null) q.setParameter("campusId", campusId);
            Number n = (Number) q.getSingleResult();
            if (n.longValue() == 0) throw new FinanceBusinessException("La classe ne correspond pas à l'année/cycle/niveau/campus du tarif.");
        }
    }

    private void exists(String table, Long id, String label) {
        if (id == null) throw new FinanceBusinessException(label + " obligatoire.");
        Number n = (Number) em.createNativeQuery("select count(*) from " + table + " where id=:id").setParameter("id", id).getSingleResult();
        if (n.longValue() == 0) throw new FinanceNotFoundException(label + " introuvable : " + id);
    }
}
