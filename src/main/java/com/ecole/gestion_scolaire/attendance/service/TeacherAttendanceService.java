package com.ecole.gestion_scolaire.attendance.service;

import com.ecole.gestion_scolaire.attendance.dto.*;
import com.ecole.gestion_scolaire.attendance.entity.ClassSession;
import com.ecole.gestion_scolaire.attendance.entity.TeacherAttendance;
import com.ecole.gestion_scolaire.attendance.enums.*;
import com.ecole.gestion_scolaire.attendance.repository.ClassSessionRepository;
import com.ecole.gestion_scolaire.attendance.repository.TeacherAttendanceRepository;
import com.ecole.gestion_scolaire.common.dto.PageResponse;
import com.ecole.gestion_scolaire.common.exception.BusinessRuleException;
import com.ecole.gestion_scolaire.common.exception.ResourceNotFoundException;
import com.ecole.gestion_scolaire.identity.entity.UserAccount;
import com.ecole.gestion_scolaire.identity.repository.UserAccountRepository;
import com.ecole.gestion_scolaire.timetable.entity.ScheduleEntry;
import com.ecole.gestion_scolaire.timetable.enums.ScheduleEntryStatus;
import com.ecole.gestion_scolaire.timetable.enums.SchoolDay;
import com.ecole.gestion_scolaire.timetable.repository.ScheduleEntryRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class TeacherAttendanceService {
    private final ScheduleEntryRepository schedules;
    private final ClassSessionRepository sessions;
    private final TeacherAttendanceRepository attendances;
    private final UserAccountRepository users;
    private final AttendanceEmailService attendanceEmailService;

    public TeacherAttendanceService(ScheduleEntryRepository schedules,
                                    ClassSessionRepository sessions,
                                    TeacherAttendanceRepository attendances,
                                    UserAccountRepository users,
                                    AttendanceEmailService attendanceEmailService) {
        this.schedules = schedules;
        this.sessions = sessions;
        this.attendances = attendances;
        this.users = users;
        this.attendanceEmailService = attendanceEmailService;
    }

    @Transactional(readOnly = true)
    public List<ExpectedTeacherAttendanceResponse> expected(LocalDate date, Long timeSlotId) {
        SchoolDay schoolDay = toSchoolDay(date.getDayOfWeek());
        if (schoolDay == null) return List.of();

        var expected = schedules.findExpectedForAttendance(
                schoolDay, timeSlotId, date, ScheduleEntryStatus.ACTIVE);

        var scheduleIds = expected.stream().map(ScheduleEntry::getId).toList();
        Map<Long, ClassSession> sessionBySchedule = new HashMap<>();
        for (Long scheduleId : scheduleIds) {
            sessions.findByScheduleEntryIdAndSessionDate(scheduleId, date)
                    .ifPresent(s -> sessionBySchedule.put(scheduleId, s));
        }

        var sessionIds = sessionBySchedule.values().stream().map(ClassSession::getId).toList();
        Map<Long, TeacherAttendance> attendanceBySession = sessionIds.isEmpty()
                ? Map.of()
                : attendances.findByClassSessionIdIn(sessionIds).stream()
                    .collect(Collectors.toMap(a -> a.getClassSession().getId(), Function.identity()));

        return expected.stream().map(e -> {
            var a = e.getTeachingAssignment();
            var teacher = a.getTeacher();
            var person = teacher.getEmployee().getPerson();
            var session = sessionBySchedule.get(e.getId());
            var attendance = session == null ? null : attendanceBySession.get(session.getId());
            return new ExpectedTeacherAttendanceResponse(
                    session == null ? null : session.getId(),
                    e.getId(), date,
                    e.getTimeSlot().getId(), e.getTimeSlot().getCode(), e.getTimeSlot().getStartTime(), e.getTimeSlot().getEndTime(),
                    teacher.getId(), person.getFirstName() + " " + person.getLastName(),
                    a.getClassGroup().getId(), a.getClassGroup().getName(),
                    a.getSubject().getId(), a.getSubject().getName(),
                    e.getRoom() == null ? null : e.getRoom().getId(),
                    e.getRoom() == null ? null : e.getRoom().getName(),
                    attendance == null ? null : attendance.getId(),
                    attendance == null ? null : attendance.getAttendanceStatus(),
                    attendance == null ? null : attendance.getLateMinutes(),
                    attendance == null ? null : attendance.getReason(),
                    attendance == null ? null : attendance.getNotes(),
                    attendance == null ? null : attendance.getValidationStatus()
            );
        }).toList();
    }

    public List<TeacherAttendanceResponse> saveBatch(TeacherAttendanceBatchRequest request) {
        UserAccount current = currentUser();
        List<TeacherAttendanceResponse> result = new ArrayList<>();
        Set<String> unique = new HashSet<>();

        for (var item : request.attendances()) {
            String key = item.scheduleEntryId() + "@" + item.date();
            if (!unique.add(key)) {
                throw new BusinessRuleException("Le même cours apparaît plusieurs fois dans la saisie d'assiduité.");
            }
            validateStatus(item);
            var schedule = getExpectedSchedule(item.scheduleEntryId(), item.date());
            var session = getOrCreateSession(schedule, item.date());
            var teacher = schedule.getTeachingAssignment().getTeacher();

            var attendance = attendances.findByClassSessionIdAndTeacherId(session.getId(), teacher.getId())
                    .orElseGet(TeacherAttendance::new);
            TeacherAttendanceStatus previousStatus = attendance.getId() == null ? null : attendance.getAttendanceStatus();

            attendance.setClassSession(session);
            attendance.setTeacher(teacher);
            attendance.setAttendanceStatus(item.status());
            attendance.setLateMinutes(item.status() == TeacherAttendanceStatus.RETARD ? item.lateMinutes() : 0);
            attendance.setReason(blankToNull(item.reason()));
            attendance.setNotes(blankToNull(item.notes()));
            // La saisie fait foi : l'enregistrement est immédiatement validé.
            // Une nouvelle sauvegarde de la même séance permet également de corriger la saisie
            // sans passer par un workflow séparé de validation/réouverture.
            attendance.setValidationStatus(AttendanceValidationStatus.VALIDATED);
            attendance.setRecordedBy(current);
            attendance.setRecordedAt(OffsetDateTime.now());
            attendance.setValidatedBy(current);
            attendance.setValidatedAt(OffsetDateTime.now());
            attendance = attendances.save(attendance);
            if (item.status() == TeacherAttendanceStatus.ABSENT && previousStatus != TeacherAttendanceStatus.ABSENT) {
                attendanceEmailService.teacherAbsent(attendance);
            }
            result.add(map(attendance));
        }
        return result;
    }

    public TeacherAttendanceResponse validate(Long id) {
        var attendance = getAttendance(id);
        attendance.setValidationStatus(AttendanceValidationStatus.VALIDATED);
        attendance.setValidatedBy(currentUser());
        attendance.setValidatedAt(OffsetDateTime.now());
        return map(attendances.save(attendance));
    }

    public TeacherAttendanceResponse reject(Long id) {
        var attendance = getAttendance(id);
        attendance.setValidationStatus(AttendanceValidationStatus.REJECTED);
        attendance.setValidatedBy(currentUser());
        attendance.setValidatedAt(OffsetDateTime.now());
        return map(attendances.save(attendance));
    }

    public TeacherAttendanceResponse reopen(Long id) {
        var attendance = getAttendance(id);
        attendance.setValidationStatus(AttendanceValidationStatus.PENDING);
        attendance.setValidatedBy(null);
        attendance.setValidatedAt(null);
        return map(attendances.save(attendance));
    }

    @Transactional(readOnly = true)
    public PageResponse<TeacherAttendanceResponse> history(LocalDate from, LocalDate to, Long teacherId,
                                                            TeacherAttendanceStatus status,
                                                            AttendanceValidationStatus validationStatus,
                                                            int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        var pageable = PageRequest.of(Math.max(page, 0), safeSize,
                Sort.by(Sort.Direction.DESC, "classSession.sessionDate").and(Sort.by("id").descending()));

        Specification<TeacherAttendance> spec = (root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            var session = root.join("classSession");
            if (from != null) predicates.add(cb.greaterThanOrEqualTo(session.get("sessionDate"), from));
            if (to != null) predicates.add(cb.lessThanOrEqualTo(session.get("sessionDate"), to));
            if (teacherId != null) predicates.add(cb.equal(root.get("teacher").get("id"), teacherId));
            if (status != null) predicates.add(cb.equal(root.get("attendanceStatus"), status));
            if (validationStatus != null) predicates.add(cb.equal(root.get("validationStatus"), validationStatus));
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        var result = attendances.findAll(spec, pageable).map(this::map);
        return PageResponse.from(result);
    }

    @Transactional(readOnly = true)
    public TeacherAttendanceSummaryResponse summary(LocalDate from, LocalDate to, Long teacherId) {
        Specification<TeacherAttendance> spec = (root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            var session = root.join("classSession");
            if (from != null) predicates.add(cb.greaterThanOrEqualTo(session.get("sessionDate"), from));
            if (to != null) predicates.add(cb.lessThanOrEqualTo(session.get("sessionDate"), to));
            if (teacherId != null) predicates.add(cb.equal(root.get("teacher").get("id"), teacherId));
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        var list = attendances.findAll(spec);
        return new TeacherAttendanceSummaryResponse(
                list.size(),
                list.stream().filter(a -> a.getAttendanceStatus() == TeacherAttendanceStatus.PRESENT).count(),
                list.stream().filter(a -> a.getAttendanceStatus() == TeacherAttendanceStatus.ABSENT).count(),
                list.stream().filter(a -> a.getAttendanceStatus() == TeacherAttendanceStatus.RETARD).count(),
                list.stream().filter(a -> a.getValidationStatus() == AttendanceValidationStatus.PENDING).count(),
                list.stream().filter(a -> a.getValidationStatus() == AttendanceValidationStatus.VALIDATED).count()
        );
    }

    private ScheduleEntry getExpectedSchedule(Long scheduleEntryId, LocalDate date) {
        var schedule = schedules.findById(scheduleEntryId)
                .orElseThrow(() -> new ResourceNotFoundException("Cours planifié introuvable"));
        if (schedule.getStatus() != ScheduleEntryStatus.ACTIVE) {
            throw new BusinessRuleException("Ce cours n'est plus actif dans l'emploi du temps.");
        }
        var expectedDay = toSchoolDay(date.getDayOfWeek());
        if (expectedDay == null || schedule.getDayOfWeek() != expectedDay) {
            throw new BusinessRuleException("La date sélectionnée ne correspond pas au jour du cours.");
        }
        if (date.isBefore(schedule.getValidFrom()) || (schedule.getValidUntil() != null && date.isAfter(schedule.getValidUntil()))) {
            throw new BusinessRuleException("Le cours n'est pas valide à cette date.");
        }
        var assignment = schedule.getTeachingAssignment();
        if (date.isBefore(assignment.getStartDate()) || (assignment.getEndDate() != null && date.isAfter(assignment.getEndDate()))) {
            throw new BusinessRuleException("L'affectation pédagogique n'est pas valide à cette date.");
        }
        return schedule;
    }

    private ClassSession getOrCreateSession(ScheduleEntry schedule, LocalDate date) {
        return sessions.findByScheduleEntryIdAndSessionDate(schedule.getId(), date).orElseGet(() -> {
            var a = schedule.getTeachingAssignment();
            var s = new ClassSession();
            s.setScheduleEntry(schedule);
            s.setSessionDate(date);
            s.setTeacher(a.getTeacher());
            s.setClassGroup(a.getClassGroup());
            s.setSubject(a.getSubject());
            s.setTimeSlot(schedule.getTimeSlot());
            s.setRoom(schedule.getRoom());
            s.setStatus(ClassSessionStatus.PLANNED);
            s.setGeneratedFromSchedule(true);
            return sessions.save(s);
        });
    }

    private void validateStatus(TeacherAttendanceSaveRequest item) {
        if (item.status() == TeacherAttendanceStatus.RETARD) {
            if (item.lateMinutes() == null || item.lateMinutes() <= 0) {
                throw new BusinessRuleException("Le nombre de minutes de retard est obligatoire et doit être supérieur à zéro.");
            }
        }
    }

    private TeacherAttendance getAttendance(Long id) {
        return attendances.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assiduité enseignant introuvable"));
    }

    private UserAccount currentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) throw new BusinessRuleException("Utilisateur courant introuvable");
        return users.findByUsername(auth.getName())
                .orElseThrow(() -> new BusinessRuleException("Utilisateur courant introuvable"));
    }

    private TeacherAttendanceResponse map(TeacherAttendance a) {
        var s = a.getClassSession();
        var person = a.getTeacher().getEmployee().getPerson();
        return new TeacherAttendanceResponse(
                a.getId(), s.getId(), s.getSessionDate(),
                a.getTeacher().getId(), person.getFirstName() + " " + person.getLastName(),
                s.getClassGroup().getId(), s.getClassGroup().getName(),
                s.getSubject().getId(), s.getSubject().getName(),
                s.getTimeSlot().getId(), s.getTimeSlot().getCode(), s.getTimeSlot().getStartTime(), s.getTimeSlot().getEndTime(),
                a.getAttendanceStatus(), a.getLateMinutes(), a.getReason(), a.getNotes(), a.getValidationStatus(),
                a.getRecordedBy() == null ? null : a.getRecordedBy().getUsername(), a.getRecordedAt(),
                a.getValidatedBy() == null ? null : a.getValidatedBy().getUsername(), a.getValidatedAt()
        );
    }

    private SchoolDay toSchoolDay(DayOfWeek day) {
        return switch (day) {
            case SUNDAY -> SchoolDay.SUNDAY;
            case MONDAY -> SchoolDay.MONDAY;
            case TUESDAY -> SchoolDay.TUESDAY;
            case WEDNESDAY -> SchoolDay.WEDNESDAY;
            case THURSDAY -> SchoolDay.THURSDAY;
            default -> null;
        };
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
