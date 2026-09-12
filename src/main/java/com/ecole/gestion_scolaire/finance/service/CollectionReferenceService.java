package com.ecole.gestion_scolaire.finance.service;

import com.ecole.gestion_scolaire.finance.dto.collection.GuardianStudentResponse;
import com.ecole.gestion_scolaire.finance.dto.collection.StudentOpenChargeResponse;
import com.ecole.gestion_scolaire.finance.entity.StudentCharge;
import com.ecole.gestion_scolaire.finance.exception.FinanceBusinessException;
import com.ecole.gestion_scolaire.finance.repository.StudentChargeRepository;
import com.ecole.gestion_scolaire.registration.entity.StudentEnrollment;
import com.ecole.gestion_scolaire.registration.repository.RegistrationCaseRepository;
import com.ecole.gestion_scolaire.registration.repository.StudentEnrollmentRepository;
import com.ecole.gestion_scolaire.school.repository.ClassGroupRepository;
import com.ecole.gestion_scolaire.student.repository.StudentGuardianRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class CollectionReferenceService {
    private final StudentGuardianRepository links;
    private final StudentEnrollmentRepository enrollments;
    private final StudentChargeRepository charges;
    private final RegistrationCaseRepository registrations;
    private final ClassGroupRepository classes;
    private final StudentChargeService chargeService;

    public CollectionReferenceService(StudentGuardianRepository links,
                                      StudentEnrollmentRepository enrollments,
                                      StudentChargeRepository charges,
                                      RegistrationCaseRepository registrations,
                                      ClassGroupRepository classes,
                                      StudentChargeService chargeService) {
        this.links = links;
        this.enrollments = enrollments;
        this.charges = charges;
        this.registrations = registrations;
        this.classes = classes;
        this.chargeService = chargeService;
    }

    @Transactional(readOnly = true)
    public List<GuardianStudentResponse> students(Long guardianId) {
        return links.findByGuardianIdAndActiveTrueOrderByIdAsc(guardianId).stream().map(link -> {
            var student = link.getStudent();
            var person = student.getPerson();
            StudentEnrollment enrollment = enrollments.findByStudentIdOrderByEnrollmentDateDescIdDesc(student.getId())
                    .stream().filter(e -> "ACTIVE".equals(e.getStatus().name())).findFirst().orElse(null);
            String className = null, levelName = null, campusName = null;
            Long enrollmentId = null, schoolYearId = null, classGroupId = null;
            if (enrollment != null) {
                enrollmentId = enrollment.getId(); schoolYearId = enrollment.getSchoolYearId(); classGroupId = enrollment.getCurrentClassGroupId();
                var cg = classes.findById(classGroupId).orElse(null);
                if (cg != null) {
                    className = cg.getName();
                    levelName = cg.getLevel() == null ? null : cg.getLevel().getName();
                    campusName = cg.getCampus() == null ? null : cg.getCampus().getName();
                }
            }
            return new GuardianStudentResponse(student.getId(), student.getStudentNumber(), person.getLastName(), person.getFirstName(), enrollmentId, schoolYearId, classGroupId, className, levelName, campusName);
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<StudentOpenChargeResponse> openCharges(Long guardianId, Long studentId, Long schoolYearId) {
        assertGuardianStudent(guardianId, studentId);
        var result = new ArrayList<StudentOpenChargeResponse>();
        for (var enrollment : enrollments.findByStudentIdOrderByEnrollmentDateDescIdDesc(studentId)) {
            if (schoolYearId != null && !schoolYearId.equals(enrollment.getSchoolYearId())) continue;
            for (var charge : charges.findByStudentEnrollmentIdOrderByDueDateAscIdAsc(enrollment.getId())) {
                addIfOpen(result, charge, studentId);
            }
        }
        // Registration charges must remain payable before a StudentEnrollment exists.
        // When schoolYearId is absent (typical for a new registration), search the student's
        // registration cases directly and keep only those belonging to the selected guardian.
        if (schoolYearId != null) {
            registrations.findByStudentIdAndSchoolYearId(studentId, schoolYearId).ifPresent(reg -> {
                if (reg.getGuardian().getId().equals(guardianId)) {
                    charges.findByRegistrationCaseIdOrderByDueDateAscIdAsc(reg.getId())
                            .forEach(c -> addIfOpen(result, c, studentId));
                }
            });
        } else {
            registrations.findByStudentIdOrderByCreatedAtDescIdDesc(studentId).stream()
                    .filter(reg -> reg.getGuardian().getId().equals(guardianId))
                    .forEach(reg -> charges.findByRegistrationCaseIdOrderByDueDateAscIdAsc(reg.getId())
                            .forEach(c -> addIfOpen(result, c, studentId)));
        }
        result.sort(Comparator.comparing(StudentOpenChargeResponse::dueDate, Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(StudentOpenChargeResponse::chargeId));
        return result;
    }

    @Transactional(readOnly = true)
    public List<StudentOpenChargeResponse> openChargesForGuardian(Long guardianId, Long schoolYearId) {
        var result = new ArrayList<StudentOpenChargeResponse>();
        for (var student : students(guardianId)) {
            result.addAll(openCharges(guardianId, student.studentId(), schoolYearId));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public void assertGuardianCanPayCharge(Long guardianId, StudentCharge charge) {
        if (charge.getStudentEnrollmentId() != null) {
            var enrollment = enrollments.findById(charge.getStudentEnrollmentId())
                    .orElseThrow(() -> new FinanceBusinessException("Scolarisation liée à la créance introuvable."));
            assertGuardianStudent(guardianId, enrollment.getStudent().getId());
            return;
        }
        if (charge.getRegistrationCaseId() != null) {
            var registration = registrations.findById(charge.getRegistrationCaseId())
                    .orElseThrow(() -> new FinanceBusinessException("Dossier lié à la créance introuvable."));
            if (!registration.getGuardian().getId().equals(guardianId))
                throw new FinanceBusinessException("Cette créance n'appartient pas au responsable sélectionné.");
            return;
        }
        throw new FinanceBusinessException("Créance sans propriétaire financier identifiable.");
    }

    private void assertGuardianStudent(Long guardianId, Long studentId) {
        if (!links.existsByStudentIdAndGuardianIdAndActiveTrue(studentId, guardianId))
            throw new FinanceBusinessException("Cet élève n'est pas lié activement au responsable sélectionné.");
    }

    private void addIfOpen(List<StudentOpenChargeResponse> result, StudentCharge charge, Long studentId) {
        var response = chargeService.toResponse(charge);
        if ("CANCELLED".equals(response.status()) || "PAID".equals(response.status()) || response.remainingAmount().signum() <= 0) return;
        var enrollment = charge.getStudentEnrollmentId() == null ? null : enrollments.findById(charge.getStudentEnrollmentId()).orElse(null);
        var student = enrollment == null ? registrations.findById(charge.getRegistrationCaseId()).map(r -> r.getStudent()).orElse(null) : enrollment.getStudent();
        if (student == null || !student.getId().equals(studentId)) return;
        var p = student.getPerson();
        result.add(new StudentOpenChargeResponse(charge.getId(), student.getId(), charge.getStudentEnrollmentId(), charge.getRegistrationCaseId(), student.getStudentNumber(), p.getLastName(), p.getFirstName(), charge.getLabel(), charge.getFeeTypeId(), response.finalAmount(), response.paidAmount(), response.remainingAmount(), charge.getDueDate(), charge.getBillingPeriodStart(), charge.getBillingPeriodEnd(), response.status()));
    }
}
