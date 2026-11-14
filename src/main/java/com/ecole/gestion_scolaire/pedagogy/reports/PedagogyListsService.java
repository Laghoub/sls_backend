package com.ecole.gestion_scolaire.pedagogy.reports;

import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class PedagogyListsService {
    private final NamedParameterJdbcTemplate jdbc;
    public PedagogyListsService(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    private MapSqlParameterSource params(Long yearId, Long cycleId, Long classId, Long subjectId) {
        return new MapSqlParameterSource().addValue("yearId", yearId)
                .addValue("cycleId", cycleId).addValue("classId", classId).addValue("subjectId", subjectId);
    }

    private String filters(Long cycleId, Long classId, Long subjectId) {
        StringBuilder sql = new StringBuilder();
        if (cycleId != null) sql.append(" AND cy.id = :cycleId ");
        if (classId != null) sql.append(" AND cg.id = :classId ");
        if (subjectId != null) sql.append(" AND sub.id = :subjectId ");
        return sql.toString();
    }

    public List<Map<String,Object>> students(Long yearId, Long cycleId, Long classId) {
        String sql = "SELECT s.id AS student_id, s.student_number AS matricule, p.last_name AS nom, " +
                "p.first_name AS prenom, p.birth_date AS date_naissance, cg.id AS classe_id, " +
                "cg.name AS classe, cy.id AS cycle_id, cy.name AS cycle, sy.label AS annee_scolaire " +
                "FROM student_enrollment se JOIN student s ON s.id=se.student_id " +
                "JOIN person p ON p.id=s.person_id JOIN school_year sy ON sy.id=se.school_year_id " +
                "JOIN class_group cg ON cg.id=se.current_class_group_id " +
                "JOIN level l ON l.id=cg.level_id JOIN cycle cy ON cy.id=l.cycle_id " +
                "JOIN registration_case rc ON rc.student_id=s.id AND rc.school_year_id=se.school_year_id " +
                "WHERE se.status='ACTIVE' AND rc.status='FINALISE' " +
                "AND se.school_year_id=COALESCE(:yearId, (SELECT id FROM school_year WHERE status='ACTIVE' ORDER BY start_date DESC LIMIT 1)) " +
                filters(cycleId, classId, null) + " ORDER BY cy.display_order, l.display_order, cg.name, p.last_name, p.first_name, s.id";
        return jdbc.queryForList(sql, params(yearId,cycleId,classId,null));
    }
    public List<Map<String,Object>> teachers(Long yearId, Long cycleId, Long classId, Long subjectId) {
        String sql = "SELECT DISTINCT t.id AS teacher_id, e.employee_number AS matricule, " +
                "p.last_name AS nom, p.first_name AS prenom, cg.id AS classe_id, cg.name AS classe, " +
                "cy.id AS cycle_id, cy.name AS cycle, sub.id AS matiere_id, sub.name AS matiere, " +
                "sy.label AS annee_scolaire " +
                "FROM teaching_assignment ta JOIN teacher t ON t.id=ta.teacher_id " +
                "JOIN employee e ON e.id=t.employee_id JOIN person p ON p.id=e.person_id " +
                "JOIN school_year sy ON sy.id=ta.school_year_id " +
                "JOIN class_group cg ON cg.id=ta.class_group_id " +
                "JOIN level l ON l.id=cg.level_id JOIN cycle cy ON cy.id=l.cycle_id " +
                "JOIN subject sub ON sub.id=ta.subject_id " +
                "WHERE ta.status='ACTIVE' AND t.status='ACTIVE' " +
                "AND (ta.end_date IS NULL OR ta.end_date >= CURRENT_DATE) " +
                "AND ta.school_year_id=COALESCE(:yearId, (SELECT id FROM school_year WHERE status='ACTIVE' ORDER BY start_date DESC LIMIT 1)) " +
                filters(cycleId, classId, subjectId) +
                "ORDER BY cycle, classe, nom, prenom, matiere";
        return jdbc.queryForList(sql, params(yearId,cycleId,classId,subjectId));
    }
}
