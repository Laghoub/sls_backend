package com.ecole.gestion_scolaire.registration.service;

import com.ecole.gestion_scolaire.common.dto.PageResponse;
import com.ecole.gestion_scolaire.finance.service.BillingScheduleService;
import com.ecole.gestion_scolaire.registration.dto.*;
import com.ecole.gestion_scolaire.registration.entity.*;
import com.ecole.gestion_scolaire.registration.enums.*;
import com.ecole.gestion_scolaire.registration.exception.RegistrationValidationException;
import com.ecole.gestion_scolaire.registration.repository.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@Transactional(readOnly = true)
public class StudentEnrollmentService {

    private final StudentEnrollmentRepository repo;
    private final EnrollmentClassHistoryRepository history;
    private final RegistrationReferenceValidator refs;
    private final BillingScheduleService billingSchedules;

    public StudentEnrollmentService(
            StudentEnrollmentRepository repo,
            EnrollmentClassHistoryRepository history,
            RegistrationReferenceValidator refs,
            BillingScheduleService billingSchedules
    ) {
        this.repo = repo;
        this.history = history;
        this.refs = refs;
        this.billingSchedules = billingSchedules;
    }

    public StudentEnrollmentResponse findByStudentAndYear(Long studentId, Long schoolYearId) {
        return repo.findByStudentIdAndSchoolYearId(studentId, schoolYearId)
                .map(this::map)
                .orElse(null);
    }

    public PageResponse<StudentEnrollmentResponse> byYear(Long schoolYearId, int page, int size) {
        page = Math.max(0, page);
        size = Math.min(Math.max(1, size), 100);

        return PageResponse.from(
                repo.findBySchoolYearId(
                        schoolYearId,
                        PageRequest.of(
                                page,
                                size,
                                Sort.by(Sort.Direction.DESC, "createdAt")
                        )
                ).map(this::map)
        );
    }

    /**
     * Crée la scolarisation annuelle à partir d'un dossier d'inscription finalisé.
     *
     * Dès que la scolarisation ACTIVE est créée, Finance génère automatiquement
     * les créances de scolarité applicables (mensualités, trimestrialités, etc.).
     *
     * L'opération reste dans la même transaction :
     * - aucune double génération grâce aux contrôles de BillingScheduleService ;
     * - si une erreur métier empêche réellement la génération, la finalisation
     *   n'est pas commitée partiellement ;
     * - s'il n'existe encore aucun tarif SCOLARITE applicable, la scolarisation
     *   est créée normalement et 0 échéance est générée. Une génération de
     *   rattrapage pourra être lancée plus tard.
     */
    @Transactional
    public StudentEnrollmentResponse createFromRegistration(
            RegistrationCase registration,
            StudentEnrollmentCreateRequest request
    ) {
        if (repo.existsByStudentIdAndSchoolYearId(
                registration.getStudent().getId(),
                registration.getSchoolYearId()
        )) {
            throw new RegistrationValidationException(
                    "L’élève est déjà scolarisé pour cette année."
            );
        }

        refs.classGroup(request.classGroupId());
        refs.classMatches(
                request.classGroupId(),
                registration.getSchoolYearId(),
                registration.getRequestedLevelId()
        );

        var enrollment = new StudentEnrollment();
        enrollment.setStudent(registration.getStudent());
        enrollment.setSchoolYearId(registration.getSchoolYearId());
        enrollment.setCurrentClassGroupId(request.classGroupId());
        enrollment.setEnrollmentDate(
                request.enrollmentDate() == null
                        ? LocalDate.now()
                        : request.enrollmentDate()
        );
        enrollment.setEntryType(request.entryType());
        enrollment.setStatus(EnrollmentStatus.ACTIVE);

        enrollment = repo.save(enrollment);

        var classHistory = new EnrollmentClassHistory();
        classHistory.setEnrollment(enrollment);
        classHistory.setClassGroupId(request.classGroupId());
        classHistory.setStartDate(enrollment.getEnrollmentDate());
        classHistory.setReason("INSCRIPTION_INITIALE");
        history.save(classHistory);

        /*
         * Fonctionnement normal de production :
         * on ne demande pas au comptable de générer manuellement l'échéancier
         * de chaque nouvel élève. La création de la scolarisation déclenche
         * automatiquement la génération des frais de catégorie SCOLARITE.
         */
        billingSchedules.generateDefault(enrollment.getId());

        return map(enrollment);
    }

    @Transactional
    public StudentEnrollmentResponse changeClass(Long id, ClassChangeRequest request) {
        var enrollment = repo.findById(id)
                .orElseThrow(() -> new RegistrationValidationException(
                        "Scolarisation introuvable : " + id
                ));

        refs.classGroup(request.classGroupId());

        LocalDate effectiveDate = request.effectiveDate() == null
                ? LocalDate.now()
                : request.effectiveDate();

        var list = history.findByEnrollmentIdOrderByStartDateAsc(id);

        list.stream()
                .filter(h -> h.getEndDate() == null)
                .forEach(h -> {
                    h.setEndDate(effectiveDate.minusDays(1));
                    history.save(h);
                });

        enrollment.setCurrentClassGroupId(request.classGroupId());
        repo.save(enrollment);

        var classHistory = new EnrollmentClassHistory();
        classHistory.setEnrollment(enrollment);
        classHistory.setClassGroupId(request.classGroupId());
        classHistory.setStartDate(effectiveDate);
        classHistory.setReason(request.reason());
        history.save(classHistory);

        return map(enrollment);
    }

    public StudentEnrollmentResponse map(StudentEnrollment enrollment) {
        return new StudentEnrollmentResponse(
                enrollment.getId(),
                enrollment.getStudent().getId(),
                enrollment.getStudent().getStudentNumber(),
                enrollment.getSchoolYearId(),
                enrollment.getCurrentClassGroupId(),
                enrollment.getEnrollmentDate(),
                enrollment.getEntryType(),
                enrollment.getStatus(),
                enrollment.getExitDate(),
                enrollment.getExitReason(),
                enrollment.getCreatedAt(),
                enrollment.getUpdatedAt()
        );
    }
}
