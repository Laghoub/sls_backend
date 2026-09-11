package com.ecole.gestion_scolaire.student.repository;

import com.ecole.gestion_scolaire.student.entity.Guardian;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface GuardianRepository extends JpaRepository<Guardian, Long> {
    Optional<Guardian> findByPersonId(Long personId);

    boolean existsByPersonId(Long personId);

    @Query("""
        SELECT g
        FROM Guardian g
        JOIN g.person p
        WHERE
            :search IS NULL
            OR :search = ''
            OR LOWER(p.lastName)
                LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(p.firstName)
                LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(CONCAT(p.firstName, ' ', p.lastName))
                LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(CONCAT(p.lastName, ' ', p.firstName))
                LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(COALESCE(p.phone, ''))
                LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(COALESCE(p.email, ''))
                LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(g.status)
                LIKE LOWER(CONCAT('%', :search, '%'))
        """)
    Page<Guardian> search(
            @Param("search") String search,
            Pageable pageable
    );
}
