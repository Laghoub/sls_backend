package com.ecole.gestion_scolaire.school.service;

import com.ecole.gestion_scolaire.common.exception.BusinessRuleException;
import com.ecole.gestion_scolaire.common.exception.ResourceNotFoundException;
import com.ecole.gestion_scolaire.school.dto.ClassGroupCreateRequest;
import com.ecole.gestion_scolaire.school.dto.ClassGroupResponse;
import com.ecole.gestion_scolaire.school.dto.ClassGroupUpdateRequest;
import com.ecole.gestion_scolaire.school.entity.Campus;
import com.ecole.gestion_scolaire.school.entity.ClassGroup;
import com.ecole.gestion_scolaire.school.entity.Level;
import com.ecole.gestion_scolaire.school.entity.SchoolYear;
import com.ecole.gestion_scolaire.school.repository.CampusRepository;
import com.ecole.gestion_scolaire.school.repository.ClassGroupRepository;
import com.ecole.gestion_scolaire.school.repository.LevelRepository;
import com.ecole.gestion_scolaire.school.repository.SchoolYearRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ClassGroupService {

    private final ClassGroupRepository classGroupRepository;
    private final SchoolYearRepository schoolYearRepository;
    private final LevelRepository levelRepository;
    private final CampusRepository campusRepository;

    public ClassGroupService(
            ClassGroupRepository classGroupRepository,
            SchoolYearRepository schoolYearRepository,
            LevelRepository levelRepository,
            CampusRepository campusRepository
    ) {
        this.classGroupRepository = classGroupRepository;
        this.schoolYearRepository = schoolYearRepository;
        this.levelRepository = levelRepository;
        this.campusRepository = campusRepository;
    }

    public ClassGroupResponse findById(Long id) {

        return toResponse(
                getEntityById(id)
        );
    }

    public List<ClassGroupResponse> findBySchoolYear(
            Long schoolYearId
    ) {

        ensureSchoolYearExists(schoolYearId);

        return classGroupRepository
                .findBySchoolYearIdOrderByNameAsc(
                        schoolYearId
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ClassGroupResponse> findBySchoolYearAndLevel(
            Long schoolYearId,
            Long levelId
    ) {

        ensureSchoolYearExists(schoolYearId);
        ensureLevelExists(levelId);

        return classGroupRepository
                .findBySchoolYearIdAndLevelIdOrderByNameAsc(
                        schoolYearId,
                        levelId
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ClassGroupResponse> findBySchoolYearAndCampus(
            Long schoolYearId,
            Long campusId
    ) {

        ensureSchoolYearExists(schoolYearId);
        ensureCampusExists(campusId);

        return classGroupRepository
                .findBySchoolYearIdAndCampusIdOrderByNameAsc(
                        schoolYearId,
                        campusId
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ClassGroupResponse> findBySchoolYearLevelAndCampus(
            Long schoolYearId,
            Long levelId,
            Long campusId
    ) {

        ensureSchoolYearExists(schoolYearId);
        ensureLevelExists(levelId);
        ensureCampusExists(campusId);

        return classGroupRepository
                .findBySchoolYearIdAndLevelIdAndCampusIdOrderByNameAsc(
                        schoolYearId,
                        levelId,
                        campusId
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ClassGroupResponse create(
            ClassGroupCreateRequest request
    ) {

        SchoolYear schoolYear =
                getSchoolYearById(
                        request.schoolYearId()
                );

        Level level =
                getLevelById(
                        request.levelId()
                );

        Campus campus =
                getCampusById(
                        request.campusId()
                );

        String normalizedCode =
                normalizeCode(request.code());

        validateCodeAvailable(
                schoolYear.getId(),
                normalizedCode,
                null
        );

        ClassGroup classGroup =
                new ClassGroup();

        classGroup.setSchoolYear(schoolYear);
        classGroup.setLevel(level);
        classGroup.setCampus(campus);

        classGroup.setCode(
                normalizedCode
        );

        classGroup.setName(
                normalizeRequired(request.name())
        );

        classGroup.setCapacity(
                request.capacity()
        );

        classGroup.setStatus(
                normalizeUppercase(request.status())
        );

        return toResponse(
                classGroupRepository.save(classGroup)
        );
    }

    @Transactional
    public ClassGroupResponse update(
            Long id,
            ClassGroupUpdateRequest request
    ) {

        ClassGroup classGroup =
                getEntityById(id);

        SchoolYear schoolYear =
                getSchoolYearById(
                        request.schoolYearId()
                );

        Level level =
                getLevelById(
                        request.levelId()
                );

        Campus campus =
                getCampusById(
                        request.campusId()
                );

        String normalizedCode =
                normalizeCode(request.code());

        validateCodeAvailable(
                schoolYear.getId(),
                normalizedCode,
                id
        );

        classGroup.setSchoolYear(schoolYear);
        classGroup.setLevel(level);
        classGroup.setCampus(campus);

        classGroup.setCode(
                normalizedCode
        );

        classGroup.setName(
                normalizeRequired(request.name())
        );

        classGroup.setCapacity(
                request.capacity()
        );

        classGroup.setStatus(
                normalizeUppercase(request.status())
        );

        return toResponse(
                classGroupRepository.save(classGroup)
        );
    }

    private ClassGroup getEntityById(
            Long id
    ) {

        return classGroupRepository
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Classe introuvable avec l'identifiant : "
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

    private Level getLevelById(
            Long id
    ) {

        return levelRepository
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Niveau introuvable avec l'identifiant : "
                                        + id
                        )
                );
    }

    private Campus getCampusById(
            Long id
    ) {

        return campusRepository
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Campus introuvable avec l'identifiant : "
                                        + id
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

    private void ensureLevelExists(
            Long id
    ) {

        if (!levelRepository.existsById(id)) {

            throw new ResourceNotFoundException(
                    "Niveau introuvable avec l'identifiant : "
                            + id
            );
        }
    }

    public List<ClassGroupResponse> findAll() {

        return classGroupRepository
                .findAll()
                .stream()
                .map(this::toResponse)
                .toList();
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

    private void validateCodeAvailable(
            Long schoolYearId,
            String code,
            Long currentId
    ) {

        classGroupRepository
                .findBySchoolYearIdAndCode(
                        schoolYearId,
                        code
                )
                .ifPresent(existing -> {

                    if (
                            currentId == null
                                    || !existing
                                    .getId()
                                    .equals(currentId)
                    ) {

                        throw new BusinessRuleException(
                                "Une classe avec le code '"
                                        + code
                                        + "' existe déjà pour cette année scolaire."
                        );
                    }
                });
    }

    private String normalizeCode(
            String value
    ) {

        return value
                .trim()
                .toUpperCase();
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

    private ClassGroupResponse toResponse(
            ClassGroup classGroup
    ) {

        return new ClassGroupResponse(

                classGroup.getId(),

                classGroup.getSchoolYear().getId(),
                classGroup.getSchoolYear().getCode(),
                classGroup.getSchoolYear().getLabel(),

                classGroup.getLevel().getId(),
                classGroup.getLevel().getCode(),
                classGroup.getLevel().getName(),

                classGroup.getLevel().getCycle().getId(),
                classGroup.getLevel().getCycle().getCode(),
                classGroup.getLevel().getCycle().getName(),

                classGroup.getCampus().getId(),
                classGroup.getCampus().getCode(),
                classGroup.getCampus().getName(),

                classGroup.getCode(),
                classGroup.getName(),
                classGroup.getCapacity(),
                classGroup.getStatus(),
                classGroup.getCreatedAt()
        );
    }
}