package com.ecole.gestion_scolaire.school.repository;

import com.ecole.gestion_scolaire.school.entity.ClassGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClassGroupRepository
        extends JpaRepository<ClassGroup, Long> {

    Optional<ClassGroup> findBySchoolYearIdAndCode(
            Long schoolYearId,
            String code
    );

    boolean existsBySchoolYearIdAndCode(
            Long schoolYearId,
            String code
    );

    List<ClassGroup> findBySchoolYearIdOrderByNameAsc(
            Long schoolYearId
    );

    List<ClassGroup> findBySchoolYearIdAndLevelIdOrderByNameAsc(
            Long schoolYearId,
            Long levelId
    );

    List<ClassGroup> findBySchoolYearIdAndCampusIdOrderByNameAsc(
            Long schoolYearId,
            Long campusId
    );

    List<ClassGroup> findBySchoolYearIdAndLevelIdAndCampusIdOrderByNameAsc(
            Long schoolYearId,
            Long levelId,
            Long campusId
    );
}