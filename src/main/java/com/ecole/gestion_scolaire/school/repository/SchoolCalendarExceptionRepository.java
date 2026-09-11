package com.ecole.gestion_scolaire.school.repository;

import com.ecole.gestion_scolaire.school.entity.SchoolCalendarException;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface SchoolCalendarExceptionRepository
        extends JpaRepository<SchoolCalendarException, Long> {

    List<SchoolCalendarException>
    findBySchoolYearIdOrderByExceptionDateAsc(
            Long schoolYearId
    );

    List<SchoolCalendarException>
    findBySchoolYearIdAndExceptionDateOrderByIdAsc(
            Long schoolYearId,
            LocalDate exceptionDate
    );

    List<SchoolCalendarException>
    findBySchoolYearIdAndCampusIdOrderByExceptionDateAsc(
            Long schoolYearId,
            Long campusId
    );

    List<SchoolCalendarException>
    findBySchoolYearIdAndCycleIdOrderByExceptionDateAsc(
            Long schoolYearId,
            Long cycleId
    );
}