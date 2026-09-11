package com.ecole.gestion_scolaire.school.service;

import com.ecole.gestion_scolaire.common.exception.BusinessRuleException;
import com.ecole.gestion_scolaire.common.exception.ResourceNotFoundException;
import com.ecole.gestion_scolaire.school.dto.SchoolYearCreateRequest;
import com.ecole.gestion_scolaire.school.dto.SchoolYearResponse;
import com.ecole.gestion_scolaire.school.dto.SchoolYearUpdateRequest;
import com.ecole.gestion_scolaire.school.entity.SchoolYear;
import com.ecole.gestion_scolaire.school.repository.SchoolYearRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class SchoolYearService {

    private final SchoolYearRepository schoolYearRepository;

    public SchoolYearService(
            SchoolYearRepository schoolYearRepository
    ) {
        this.schoolYearRepository = schoolYearRepository;
    }

    public List<SchoolYearResponse> findAll() {

        return schoolYearRepository
                .findAll(
                        Sort.by(
                                Sort.Direction.DESC,
                                "startDate"
                        )
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public SchoolYearResponse findById(Long id) {

        return toResponse(
                getEntityById(id)
        );
    }

    public SchoolYearResponse findCurrent() {

        SchoolYear schoolYear =
                schoolYearRepository.findByCurrentYearTrue()
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Aucune année scolaire courante n'est définie."
                                )
                        );

        return toResponse(schoolYear);
    }

    @Transactional
    public SchoolYearResponse create(
            SchoolYearCreateRequest request
    ) {

        validateDates(
                request.startDate(),
                request.endDate()
        );

        validateCodeAvailable(
                request.code(),
                null
        );

        if (request.currentYear()) {
            unsetCurrentSchoolYear();
        }

        SchoolYear schoolYear = new SchoolYear();

        schoolYear.setCode(
                normalizeCode(request.code())
        );

        schoolYear.setLabel(
                request.label().trim()
        );

        schoolYear.setStartDate(
                request.startDate()
        );

        schoolYear.setEndDate(
                request.endDate()
        );

        schoolYear.setStatus(
                request.status()
        );

        schoolYear.setCurrentYear(
                request.currentYear()
        );

        SchoolYear saved =
                schoolYearRepository.save(schoolYear);

        return toResponse(saved);
    }

    @Transactional
    public SchoolYearResponse update(
            Long id,
            SchoolYearUpdateRequest request
    ) {

        SchoolYear schoolYear =
                getEntityById(id);

        validateDates(
                request.startDate(),
                request.endDate()
        );

        validateCodeAvailable(
                request.code(),
                id
        );

        schoolYear.setCode(
                normalizeCode(request.code())
        );

        schoolYear.setLabel(
                request.label().trim()
        );

        schoolYear.setStartDate(
                request.startDate()
        );

        schoolYear.setEndDate(
                request.endDate()
        );

        schoolYear.setStatus(
                request.status()
        );

        return toResponse(
                schoolYearRepository.save(schoolYear)
        );
    }

    @Transactional
    public SchoolYearResponse defineAsCurrent(
            Long id
    ) {

        SchoolYear target =
                getEntityById(id);

        if (target.isCurrentYear()) {
            return toResponse(target);
        }

        unsetCurrentSchoolYear();

        target.setCurrentYear(true);

        return toResponse(
                schoolYearRepository.save(target)
        );
    }

    private void unsetCurrentSchoolYear() {

        schoolYearRepository.findByCurrentYearTrue()
                .ifPresent(current -> {

                    current.setCurrentYear(false);

                    schoolYearRepository.save(current);
                });
    }

    private SchoolYear getEntityById(Long id) {

        return schoolYearRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Année scolaire introuvable avec l'identifiant : "
                                        + id
                        )
                );
    }

    private void validateDates(
            java.time.LocalDate startDate,
            java.time.LocalDate endDate
    ) {

        if (!endDate.isAfter(startDate)) {

            throw new BusinessRuleException(
                    "La date de fin doit être postérieure à la date de début."
            );
        }
    }

    private void validateCodeAvailable(
            String code,
            Long currentId
    ) {

        String normalizedCode =
                normalizeCode(code);

        schoolYearRepository
                .findByCode(normalizedCode)
                .ifPresent(existing -> {

                    if (
                            currentId == null
                                    || !existing.getId().equals(currentId)
                    ) {

                        throw new BusinessRuleException(
                                "Une année scolaire avec le code '"
                                        + normalizedCode
                                        + "' existe déjà."
                        );
                    }
                });
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase();
    }

    private SchoolYearResponse toResponse(
            SchoolYear schoolYear
    ) {

        return new SchoolYearResponse(
                schoolYear.getId(),
                schoolYear.getCode(),
                schoolYear.getLabel(),
                schoolYear.getStartDate(),
                schoolYear.getEndDate(),
                schoolYear.getStatus(),
                schoolYear.isCurrentYear(),
                schoolYear.getCreatedAt(),
                schoolYear.getUpdatedAt()
        );
    }
}