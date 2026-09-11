package com.ecole.gestion_scolaire.registration.repository;

import com.ecole.gestion_scolaire.registration.entity.RegistrationCase;
import com.ecole.gestion_scolaire.registration.enums.RegistrationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RegistrationCaseRepository
        extends JpaRepository<RegistrationCase, Long> {

    boolean existsByStudentIdAndSchoolYearId(
            Long studentId,
            Long schoolYearId
    );

    Optional<RegistrationCase> findByStudentIdAndSchoolYearId(Long studentId, Long schoolYearId);

    @Query("""
            SELECT r
            FROM RegistrationCase r
            JOIN r.student s
            JOIN s.person sp
            JOIN r.guardian g
            JOIN g.person gp
            WHERE (:schoolYearId IS NULL OR r.schoolYearId = :schoolYearId)
              AND (:status IS NULL OR r.status = :status)
              AND (
                    :q = ''
                    OR LOWER(sp.lastName) LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(sp.firstName) LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(s.studentNumber) LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(gp.lastName) LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(gp.firstName) LIKE LOWER(CONCAT('%', :q, '%'))
              )
            """)
    Page<RegistrationCase> search(
            @Param("q") String q,
            @Param("schoolYearId") Long schoolYearId,
            @Param("status") RegistrationStatus status,
            Pageable pageable
    );
}