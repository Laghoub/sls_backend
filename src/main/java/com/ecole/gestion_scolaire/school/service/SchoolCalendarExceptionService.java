package com.ecole.gestion_scolaire.school.service;

import com.ecole.gestion_scolaire.common.exception.BusinessRuleException;
import com.ecole.gestion_scolaire.common.exception.ResourceNotFoundException;
import com.ecole.gestion_scolaire.school.dto.SchoolCalendarExceptionCreateRequest;
import com.ecole.gestion_scolaire.school.dto.SchoolCalendarExceptionResponse;
import com.ecole.gestion_scolaire.school.dto.SchoolCalendarExceptionUpdateRequest;
import com.ecole.gestion_scolaire.school.entity.Campus;
import com.ecole.gestion_scolaire.school.entity.Cycle;
import com.ecole.gestion_scolaire.school.entity.SchoolCalendarException;
import com.ecole.gestion_scolaire.school.entity.SchoolYear;
import com.ecole.gestion_scolaire.school.repository.CampusRepository;
import com.ecole.gestion_scolaire.school.repository.CycleRepository;
import com.ecole.gestion_scolaire.school.repository.SchoolCalendarExceptionRepository;
import com.ecole.gestion_scolaire.school.repository.SchoolYearRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class SchoolCalendarExceptionService {

    private final SchoolCalendarExceptionRepository exceptionRepository;
    private final SchoolYearRepository schoolYearRepository;
    private final CampusRepository campusRepository;
    private final CycleRepository cycleRepository;

    public SchoolCalendarExceptionService(
            SchoolCalendarExceptionRepository exceptionRepository,
            SchoolYearRepository schoolYearRepository,
            CampusRepository campusRepository,
            CycleRepository cycleRepository
    ) {
        this.exceptionRepository = exceptionRepository;
        this.schoolYearRepository = schoolYearRepository;
        this.campusRepository = campusRepository;
        this.cycleRepository = cycleRepository;
    }

    public SchoolCalendarExceptionResponse findById(
            Long id
    ) {

        return toResponse(
                getEntityById(id)
        );
    }

    public List<SchoolCalendarExceptionResponse> findBySchoolYear(
            Long schoolYearId
    ) {

        ensureSchoolYearExists(schoolYearId);

        return exceptionRepository
                .findBySchoolYearIdOrderByExceptionDateAsc(
                        schoolYearId
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<SchoolCalendarExceptionResponse> findByDate(
            Long schoolYearId,
            LocalDate date
    ) {

        ensureSchoolYearExists(schoolYearId);

        return exceptionRepository
                .findBySchoolYearIdAndExceptionDateOrderByIdAsc(
                        schoolYearId,
                        date
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<SchoolCalendarExceptionResponse> findByCampus(
            Long schoolYearId,
            Long campusId
    ) {

        ensureSchoolYearExists(schoolYearId);
        ensureCampusExists(campusId);

        return exceptionRepository
                .findBySchoolYearIdAndCampusIdOrderByExceptionDateAsc(
                        schoolYearId,
                        campusId
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<SchoolCalendarExceptionResponse> findAll() {

        return exceptionRepository
                .findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }
    public List<SchoolCalendarExceptionResponse> findByCycle(
            Long schoolYearId,
            Long cycleId
    ) {

        ensureSchoolYearExists(schoolYearId);
        ensureCycleExists(cycleId);

        return exceptionRepository
                .findBySchoolYearIdAndCycleIdOrderByExceptionDateAsc(
                        schoolYearId,
                        cycleId
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public SchoolCalendarExceptionResponse create(
            SchoolCalendarExceptionCreateRequest request
    ) {

        SchoolYear schoolYear =
                getSchoolYearById(
                        request.schoolYearId()
                );

        Campus campus =
                getOptionalCampus(
                        request.campusId()
                );

        Cycle cycle =
                getOptionalCycle(
                        request.cycleId()
                );

        validateExceptionDate(
                schoolYear,
                request.exceptionDate()
        );

        SchoolCalendarException exception =
                new SchoolCalendarException();

        exception.setSchoolYear(schoolYear);
        exception.setCampus(campus);
        exception.setCycle(cycle);

        exception.setExceptionDate(
                request.exceptionDate()
        );

        exception.setExceptionType(
                normalizeUppercase(
                        request.exceptionType()
                )
        );

        exception.setLabel(
                normalizeRequired(
                        request.label()
                )
        );

        exception.setDescription(
                normalizeNullable(
                        request.description()
                )
        );

        return toResponse(
                exceptionRepository.save(exception)
        );
    }

    @Transactional
    public SchoolCalendarExceptionResponse update(
            Long id,
            SchoolCalendarExceptionUpdateRequest request
    ) {

        SchoolCalendarException exception =
                getEntityById(id);

        SchoolYear schoolYear =
                getSchoolYearById(
                        request.schoolYearId()
                );

        Campus campus =
                getOptionalCampus(
                        request.campusId()
                );

        Cycle cycle =
                getOptionalCycle(
                        request.cycleId()
                );

        validateExceptionDate(
                schoolYear,
                request.exceptionDate()
        );

        exception.setSchoolYear(schoolYear);
        exception.setCampus(campus);
        exception.setCycle(cycle);

        exception.setExceptionDate(
                request.exceptionDate()
        );

        exception.setExceptionType(
                normalizeUppercase(
                        request.exceptionType()
                )
        );

        exception.setLabel(
                normalizeRequired(
                        request.label()
                )
        );

        exception.setDescription(
                normalizeNullable(
                        request.description()
                )
        );

        return toResponse(
                exceptionRepository.save(exception)
        );
    }

    private SchoolCalendarException getEntityById(
            Long id
    ) {

        return exceptionRepository
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Exception du calendrier scolaire introuvable avec l'identifiant : "
                                        + id
                        )
                );
    }

    private SchoolYear getSchoolYearById(
            Long id
    ) {

        return schoolYearRepository
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Année scolaire introuvable avec l'identifiant : "
                                        + id
                        )
                );
    }

    private Campus getOptionalCampus(
            Long campusId
    ) {

        if (campusId == null) {
            return null;
        }

        return campusRepository
                .findById(campusId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Campus introuvable avec l'identifiant : "
                                        + campusId
                        )
                );
    }

    private Cycle getOptionalCycle(
            Long cycleId
    ) {

        if (cycleId == null) {
            return null;
        }

        return cycleRepository
                .findById(cycleId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Cycle introuvable avec l'identifiant : "
                                        + cycleId
                        )
                );
    }

    private void ensureSchoolYearExists(
            Long id
    ) {

        if (!schoolYearRepository.existsById(id)) {
            throw new ResourceNotFoundException(
                    "Année scolaire introuvable avec l'identifiant : "
                            + id
            );
        }
    }

    private void ensureCampusExists(
            Long id
    ) {

        if (!campusRepository.existsById(id)) {
            throw new ResourceNotFoundException(
                    "Campus introuvable avec l'identifiant : "
                            + id
            );
        }
    }

    private void ensureCycleExists(
            Long id
    ) {

        if (!cycleRepository.existsById(id)) {
            throw new ResourceNotFoundException(
                    "Cycle introuvable avec l'identifiant : "
                            + id
            );
        }
    }

    private void validateExceptionDate(
            SchoolYear schoolYear,
            LocalDate exceptionDate
    ) {

        if (
                exceptionDate.isBefore(
                        schoolYear.getStartDate()
                )
                        ||
                        exceptionDate.isAfter(
                                schoolYear.getEndDate()
                        )
        ) {

            throw new BusinessRuleException(
                    "La date de l'exception doit appartenir à la période de l'année scolaire."
            );
        }
    }

    private String normalizeRequired(
            String value
    ) {
        return value.trim();
    }

    private String normalizeUppercase(
            String value
    ) {
        return value
                .trim()
                .toUpperCase();
    }

    private String normalizeNullable(
            String value
    ) {

        if (value == null) {
            return null;
        }

        String normalized =
                value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }

    private SchoolCalendarExceptionResponse toResponse(
            SchoolCalendarException exception
    ) {

        Campus campus =
                exception.getCampus();

        Cycle cycle =
                exception.getCycle();

        return new SchoolCalendarExceptionResponse(

                exception.getId(),

                exception.getSchoolYear().getId(),
                exception.getSchoolYear().getCode(),
                exception.getSchoolYear().getLabel(),

                campus != null
                        ? campus.getId()
                        : null,

                campus != null
                        ? campus.getCode()
                        : null,

                campus != null
                        ? campus.getName()
                        : null,

                cycle != null
                        ? cycle.getId()
                        : null,

                cycle != null
                        ? cycle.getCode()
                        : null,

                cycle != null
                        ? cycle.getName()
                        : null,

                exception.getExceptionDate(),
                exception.getExceptionType(),
                exception.getLabel(),
                exception.getDescription()
        );
    }
}