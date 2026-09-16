package com.ecole.gestion_scolaire.timetable.service;

import com.ecole.gestion_scolaire.common.exception.BusinessRuleException;
import com.ecole.gestion_scolaire.common.exception.ResourceNotFoundException;
import com.ecole.gestion_scolaire.hr.entity.TeachingAssignment;
import com.ecole.gestion_scolaire.hr.enums.AssignmentStatus;
import com.ecole.gestion_scolaire.hr.repository.TeachingAssignmentRepository;
import com.ecole.gestion_scolaire.school.entity.Room;
import com.ecole.gestion_scolaire.school.entity.TimeSlot;
import com.ecole.gestion_scolaire.school.repository.ClassGroupRepository;
import com.ecole.gestion_scolaire.school.repository.RoomRepository;
import com.ecole.gestion_scolaire.school.repository.TimeSlotRepository;
import com.ecole.gestion_scolaire.timetable.dto.*;
import com.ecole.gestion_scolaire.timetable.entity.ScheduleEntry;
import com.ecole.gestion_scolaire.timetable.enums.ScheduleEntryStatus;
import com.ecole.gestion_scolaire.timetable.repository.ScheduleEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Service
@Transactional
public class TimetableService {
    private final ScheduleEntryRepository schedules;
    private final TeachingAssignmentRepository assignments;
    private final TimeSlotRepository timeSlots;
    private final RoomRepository rooms;
    private final ClassGroupRepository classes;

    public TimetableService(ScheduleEntryRepository schedules,
                            TeachingAssignmentRepository assignments,
                            TimeSlotRepository timeSlots,
                            RoomRepository rooms,
                            ClassGroupRepository classes) {
        this.schedules = schedules;
        this.assignments = assignments;
        this.timeSlots = timeSlots;
        this.rooms = rooms;
        this.classes = classes;
    }

    public ScheduleEntryResponse create(ScheduleEntryRequest request) {
        var entry = new ScheduleEntry();
        apply(entry, request, null);
        return map(schedules.save(entry));
    }

    public ScheduleEntryResponse update(Long id, ScheduleEntryRequest request) {
        var entry = schedules.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Créneau d'emploi du temps introuvable"));
        apply(entry, request, id);
        return map(schedules.save(entry));
    }

    public void deactivate(Long id) {
        var entry = schedules.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Créneau d'emploi du temps introuvable"));
        entry.setStatus(ScheduleEntryStatus.INACTIVE);
        schedules.save(entry);
    }

    @Transactional(readOnly = true)
    public List<ScheduleEntryResponse> byClass(Long schoolYearId, Long classGroupId) {
        ensureClassYear(classGroupId, schoolYearId);
        return schedules.findClassSchedule(schoolYearId, classGroupId, ScheduleEntryStatus.ACTIVE)
                .stream().map(this::map).toList();
    }

    @Transactional(readOnly = true)
    public List<ScheduleEntryResponse> byTeacher(Long schoolYearId, Long teacherId) {
        return schedules.findTeacherSchedule(schoolYearId, teacherId, ScheduleEntryStatus.ACTIVE)
                .stream().map(this::map).toList();
    }

    @Transactional(readOnly = true)
    public List<EligibleAssignmentResponse> eligibleAssignments(Long schoolYearId, Long classGroupId) {
        ensureClassYear(classGroupId, schoolYearId);
        return assignments.findBySchoolYearIdAndClassGroupIdAndStatus(schoolYearId, classGroupId, AssignmentStatus.ACTIVE)
                .stream().map(a -> {
                    var p = a.getTeacher().getEmployee().getPerson();
                    return new EligibleAssignmentResponse(
                            a.getId(), a.getTeacher().getId(), p.getFirstName() + " " + p.getLastName(),
                            a.getSubject().getId(), a.getSubject().getName(),
                            a.getClassGroup().getId(), a.getClassGroup().getName());
                }).toList();
    }

    public List<ScheduleEntryResponse> replaceClassGrid(TimetableGridSaveRequest request) {
        ensureClassYear(request.classGroupId(), request.schoolYearId());

        // Validation complète avant de toucher aux anciennes lignes.
        var prepared = new ArrayList<ScheduleEntry>();
        for (var item : request.entries()) {
            var entry = new ScheduleEntry();
            applyWithoutExternalClassConflict(entry, item, request.schoolYearId(), request.classGroupId());
            validateInternalGridConflicts(prepared, entry);
            prepared.add(entry);
        }

        var old = schedules.findByTeachingAssignmentClassGroupIdAndTeachingAssignmentSchoolYearIdAndStatus(
                request.classGroupId(), request.schoolYearId(), ScheduleEntryStatus.ACTIVE);
        old.forEach(e -> e.setStatus(ScheduleEntryStatus.INACTIVE));
        schedules.saveAll(old);

        // Les conflits externes (enseignant/salle) sont vérifiés après désactivation logique des lignes de la classe.
        for (var entry : prepared) validateConflicts(entry, null, true);
        return schedules.saveAll(prepared).stream().map(this::map).toList();
    }

    private void apply(ScheduleEntry entry, ScheduleEntryRequest request, Long currentId) {
        validateDates(request.validFrom(), request.validUntil());
        var assignment = getAssignment(request.teachingAssignmentId());
        var slot = getSlot(request.timeSlotId());
        var room = getRoom(request.roomId());
        validateRoomCampus(assignment, room);

        entry.setTeachingAssignment(assignment);
        entry.setDayOfWeek(request.dayOfWeek());
        entry.setTimeSlot(slot);
        entry.setRoom(room);
        entry.setValidFrom(request.validFrom());
        entry.setValidUntil(request.validUntil());
        entry.setStatus(ScheduleEntryStatus.ACTIVE);
        validateConflicts(entry, currentId, false);
    }

    private void applyWithoutExternalClassConflict(ScheduleEntry entry, ScheduleEntryRequest request,
                                                    Long schoolYearId, Long classGroupId) {
        validateDates(request.validFrom(), request.validUntil());
        var assignment = getAssignment(request.teachingAssignmentId());
        if (!assignment.getSchoolYear().getId().equals(schoolYearId)
                || !assignment.getClassGroup().getId().equals(classGroupId)) {
            throw new BusinessRuleException("Toutes les lignes doivent appartenir à la classe et à l'année scolaire sélectionnées.");
        }
        var slot = getSlot(request.timeSlotId());
        var room = getRoom(request.roomId());
        validateRoomCampus(assignment, room);
        entry.setTeachingAssignment(assignment);
        entry.setDayOfWeek(request.dayOfWeek());
        entry.setTimeSlot(slot);
        entry.setRoom(room);
        entry.setValidFrom(request.validFrom());
        entry.setValidUntil(request.validUntil());
        entry.setStatus(ScheduleEntryStatus.ACTIVE);
    }

    private TeachingAssignment getAssignment(Long id) {
        var a = assignments.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Affectation pédagogique introuvable"));
        if (a.getStatus() != AssignmentStatus.ACTIVE) {
            throw new BusinessRuleException("L'affectation pédagogique sélectionnée n'est plus active.");
        }
        if (a.getEndDate() != null && a.getEndDate().isBefore(LocalDate.now())) {
            throw new BusinessRuleException("L'affectation pédagogique sélectionnée est terminée.");
        }
        return a;
    }

    private TimeSlot getSlot(Long id) {
        var s = timeSlots.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Créneau horaire introuvable"));
        if (!s.isActive()) throw new BusinessRuleException("Le créneau horaire sélectionné est inactif.");
        return s;
    }

    private Room getRoom(Long id) {
        if (id == null) return null;
        var room = rooms.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Salle introuvable"));
        if (!room.isActive()) throw new BusinessRuleException("La salle sélectionnée est inactive.");
        return room;
    }

    private void validateRoomCampus(TeachingAssignment assignment, Room room) {
        if (room != null && !room.getCampus().getId().equals(assignment.getClassGroup().getCampus().getId())) {
            throw new BusinessRuleException("La salle sélectionnée n'appartient pas au campus de la classe.");
        }
    }

    private void ensureClassYear(Long classGroupId, Long schoolYearId) {
        var c = classes.findById(classGroupId)
                .orElseThrow(() -> new ResourceNotFoundException("Classe introuvable"));
        if (!c.getSchoolYear().getId().equals(schoolYearId)) {
            throw new BusinessRuleException("La classe n'appartient pas à l'année scolaire sélectionnée.");
        }
    }

    private void validateDates(LocalDate from, LocalDate until) {
        if (until != null && until.isBefore(from)) {
            throw new BusinessRuleException("La date de fin ne peut pas être antérieure à la date de début.");
        }
    }

    private void validateConflicts(ScheduleEntry candidate, Long currentId, boolean ignoreSameClass) {
        var a = candidate.getTeachingAssignment();
        var candidates = schedules.findActiveForConflictCheck(
                a.getSchoolYear().getId(), candidate.getDayOfWeek(), ScheduleEntryStatus.ACTIVE);

        for (var existing : candidates) {
            if (currentId != null && currentId.equals(existing.getId())) continue;
            if (ignoreSameClass && existing.getTeachingAssignment().getClassGroup().getId()
                    .equals(a.getClassGroup().getId())) continue;
            if (!dateRangesOverlap(candidate, existing)) continue;
            if (!timeRangesOverlap(candidate.getTimeSlot(), existing.getTimeSlot())) continue;

            var ea = existing.getTeachingAssignment();
            if (ea.getClassGroup().getId().equals(a.getClassGroup().getId())) {
                throw new BusinessRuleException("Conflit : la classe possède déjà un cours sur ce créneau.");
            }
            if (ea.getTeacher().getId().equals(a.getTeacher().getId())) {
                throw new BusinessRuleException("Conflit : l'enseignant est déjà occupé sur ce créneau.");
            }
            if (candidate.getRoom() != null && existing.getRoom() != null
                    && candidate.getRoom().getId().equals(existing.getRoom().getId())) {
                throw new BusinessRuleException("Conflit : la salle est déjà occupée sur ce créneau.");
            }
        }
    }

    private void validateInternalGridConflicts(List<ScheduleEntry> alreadyPrepared, ScheduleEntry candidate) {
        for (var existing : alreadyPrepared) {
            if (existing.getDayOfWeek() != candidate.getDayOfWeek()) continue;
            if (!dateRangesOverlap(candidate, existing)) continue;
            if (!timeRangesOverlap(candidate.getTimeSlot(), existing.getTimeSlot())) continue;

            var ca = candidate.getTeachingAssignment();
            var ea = existing.getTeachingAssignment();
            if (ca.getClassGroup().getId().equals(ea.getClassGroup().getId())) {
                throw new BusinessRuleException("Conflit dans la grille : deux cours occupent le même créneau de classe.");
            }
            if (ca.getTeacher().getId().equals(ea.getTeacher().getId())) {
                throw new BusinessRuleException("Conflit dans la grille : un enseignant est utilisé deux fois au même moment.");
            }
            if (candidate.getRoom() != null && existing.getRoom() != null
                    && candidate.getRoom().getId().equals(existing.getRoom().getId())) {
                throw new BusinessRuleException("Conflit dans la grille : une salle est utilisée deux fois au même moment.");
            }
        }
    }

    private boolean timeRangesOverlap(TimeSlot a, TimeSlot b) {
        LocalTime aStart = a.getStartTime();
        LocalTime aEnd = a.getEndTime();
        LocalTime bStart = b.getStartTime();
        LocalTime bEnd = b.getEndTime();
        return aStart.isBefore(bEnd) && bStart.isBefore(aEnd);
    }

    private boolean dateRangesOverlap(ScheduleEntry a, ScheduleEntry b) {
        LocalDate aEnd = a.getValidUntil() == null ? LocalDate.of(9999, 12, 31) : a.getValidUntil();
        LocalDate bEnd = b.getValidUntil() == null ? LocalDate.of(9999, 12, 31) : b.getValidUntil();
        return !aEnd.isBefore(b.getValidFrom()) && !bEnd.isBefore(a.getValidFrom());
    }

    private ScheduleEntryResponse map(ScheduleEntry e) {
        var a = e.getTeachingAssignment();
        var p = a.getTeacher().getEmployee().getPerson();
        return new ScheduleEntryResponse(
                e.getId(), a.getSchoolYear().getId(), a.getSchoolYear().getLabel(),
                a.getClassGroup().getId(), a.getClassGroup().getName(), a.getId(),
                a.getTeacher().getId(), p.getFirstName() + " " + p.getLastName(),
                a.getSubject().getId(), a.getSubject().getName(), e.getDayOfWeek().name(),
                e.getTimeSlot().getId(), e.getTimeSlot().getCode(), e.getTimeSlot().getStartTime(),
                e.getTimeSlot().getEndTime(), e.getRoom() == null ? null : e.getRoom().getId(),
                e.getRoom() == null ? null : e.getRoom().getName(),
                e.getValidFrom(), e.getValidUntil(), e.getStatus().name());
    }
}
