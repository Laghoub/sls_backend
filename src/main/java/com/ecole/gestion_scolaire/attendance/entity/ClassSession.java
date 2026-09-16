package com.ecole.gestion_scolaire.attendance.entity;

import com.ecole.gestion_scolaire.attendance.enums.ClassSessionStatus;
import com.ecole.gestion_scolaire.hr.entity.Subject;
import com.ecole.gestion_scolaire.hr.entity.Teacher;
import com.ecole.gestion_scolaire.school.entity.ClassGroup;
import com.ecole.gestion_scolaire.school.entity.Room;
import com.ecole.gestion_scolaire.school.entity.TimeSlot;
import com.ecole.gestion_scolaire.timetable.entity.ScheduleEntry;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "class_session")
public class ClassSession {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_entry_id")
    private ScheduleEntry scheduleEntry;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_group_id", nullable = false)
    private ClassGroup classGroup;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "time_slot_id", nullable = false)
    private TimeSlot timeSlot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ClassSessionStatus status;

    @Column(name = "generated_from_schedule", nullable = false)
    private boolean generatedFromSchedule;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        var now = OffsetDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
        if (status == null) status = ClassSessionStatus.PLANNED;
    }

    @PreUpdate void preUpdate() { updatedAt = OffsetDateTime.now(); }

    public Long getId() { return id; }
    public ScheduleEntry getScheduleEntry() { return scheduleEntry; }
    public void setScheduleEntry(ScheduleEntry v) { scheduleEntry = v; }
    public LocalDate getSessionDate() { return sessionDate; }
    public void setSessionDate(LocalDate v) { sessionDate = v; }
    public Teacher getTeacher() { return teacher; }
    public void setTeacher(Teacher v) { teacher = v; }
    public ClassGroup getClassGroup() { return classGroup; }
    public void setClassGroup(ClassGroup v) { classGroup = v; }
    public Subject getSubject() { return subject; }
    public void setSubject(Subject v) { subject = v; }
    public TimeSlot getTimeSlot() { return timeSlot; }
    public void setTimeSlot(TimeSlot v) { timeSlot = v; }
    public Room getRoom() { return room; }
    public void setRoom(Room v) { room = v; }
    public ClassSessionStatus getStatus() { return status; }
    public void setStatus(ClassSessionStatus v) { status = v; }
    public boolean isGeneratedFromSchedule() { return generatedFromSchedule; }
    public void setGeneratedFromSchedule(boolean v) { generatedFromSchedule = v; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
