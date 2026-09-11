package com.ecole.gestion_scolaire.school.repository;

import com.ecole.gestion_scolaire.school.entity.SchoolYear;
import com.ecole.gestion_scolaire.school.entity.enums.SchoolYearStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SchoolYearRepository
        extends JpaRepository<SchoolYear, Long> {

    Optional<SchoolYear> findByCode(String code);

    boolean existsByCode(String code);

    Optional<SchoolYear> findByCurrentYearTrue();

    List<SchoolYear> findByStatus(
            SchoolYearStatus status
    );
}