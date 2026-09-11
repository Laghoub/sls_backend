package com.ecole.gestion_scolaire.registration.service;

import com.ecole.gestion_scolaire.common.dto.PageResponse;
import com.ecole.gestion_scolaire.registration.dto.*;
import com.ecole.gestion_scolaire.registration.entity.*;
import com.ecole.gestion_scolaire.registration.enums.RegistrationStatus;
import com.ecole.gestion_scolaire.registration.exception.*;
import com.ecole.gestion_scolaire.registration.repository.*;
import com.ecole.gestion_scolaire.student.repository.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@Transactional(readOnly = true)
public class RegistrationCaseService {
    private final RegistrationCaseRepository repo;
    private final StudentRepository students;
    private final GuardianRepository guardians;
    private final RegistrationReferenceValidator refs;
    private final CurrentAccountService current;

    public RegistrationCaseService(RegistrationCaseRepository r, StudentRepository s, GuardianRepository g, RegistrationReferenceValidator refs, CurrentAccountService current) {
        repo = r;
        students = s;
        guardians = g;
        this.refs = refs;
        this.current = current;
    }

    public PageResponse<RegistrationCaseResponse> search(String q, Long year, RegistrationStatus status, int page, int size) {
        page = Math.max(page, 0);
        size = Math.min(Math.max(size, 1), 100);
        var p = repo.search(q == null ? "" : q.trim(), year, status, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))).map(this::toResponse);
        return PageResponse.from(p);
    }

    public RegistrationCaseResponse findById(Long id) {
        return toResponse(get(id));
    }

    @Transactional
    public RegistrationCaseResponse create(RegistrationCaseCreateRequest r) {
        refs.schoolYear(r.schoolYearId());
        refs.level(r.requestedLevelId());
        refs.classGroup(r.requestedClassGroupId());
        refs.classMatches(r.requestedClassGroupId(), r.schoolYearId(), r.requestedLevelId());
        if (repo.existsByStudentIdAndSchoolYearId(r.studentId(), r.schoolYearId()))
            throw new RegistrationValidationException("Un dossier existe déjà pour cet élève et cette année scolaire.");
        var s = students.findById(r.studentId()).orElseThrow(() -> new RegistrationValidationException("Élève introuvable : " + r.studentId()));
        var g = guardians.findById(r.guardianId()).orElseThrow(() -> new RegistrationValidationException("Responsable introuvable : " + r.guardianId()));
        RegistrationCase x = new RegistrationCase();
        x.setSchoolYearId(r.schoolYearId());
        x.setStudent(s);
        x.setGuardian(g);
        x.setRequestedLevelId(r.requestedLevelId());
        x.setRequestedClassGroupId(r.requestedClassGroupId());
        x.setRegistrationDate(r.registrationDate() == null ? LocalDate.now() : r.registrationDate());
        x.setStatus(RegistrationStatus.PRE_INSCRIPTION);
        x.setCreatedById(current.id());
        return toResponse(repo.save(x));
    }

    @Transactional
    public RegistrationCaseResponse update(Long id, RegistrationCaseUpdateRequest r) {
        var x = get(id);
        if (x.getStatus() == RegistrationStatus.FINALISE || x.getStatus() == RegistrationStatus.ANNULE)
            throw new InvalidRegistrationTransitionException("Un dossier finalisé ou annulé n’est plus modifiable.");
        refs.level(r.requestedLevelId());
        refs.classGroup(r.requestedClassGroupId());
        refs.classMatches(r.requestedClassGroupId(), x.getSchoolYearId(), r.requestedLevelId());
        x.setGuardian(guardians.findById(r.guardianId()).orElseThrow(() -> new RegistrationValidationException("Responsable introuvable : " + r.guardianId())));
        x.setRequestedLevelId(r.requestedLevelId());
        x.setRequestedClassGroupId(r.requestedClassGroupId());
        return toResponse(repo.save(x));
    }

    public RegistrationCase get(Long id) {
        return repo.findById(id).orElseThrow(() -> new RegistrationNotFoundException("Dossier d’inscription introuvable : " + id));
    }

    public RegistrationCaseResponse toResponse(RegistrationCase x) {
        var sp = x.getStudent().getPerson();
        var gp = x.getGuardian().getPerson();
        return new RegistrationCaseResponse(x.getId(), x.getSchoolYearId(), x.getStudent().getId(), x.getStudent().getStudentNumber(), sp.getLastName(), sp.getFirstName(), x.getGuardian().getId(), gp.getLastName(), gp.getFirstName(), x.getRequestedLevelId(), x.getRequestedClassGroupId(), x.getRegistrationDate(), x.getStatus(), x.getCreatedById(), x.getFinalizedAt(), x.getCancelledAt(), x.getCancellationReason(), x.getCreatedAt(), x.getUpdatedAt());
    }
}
