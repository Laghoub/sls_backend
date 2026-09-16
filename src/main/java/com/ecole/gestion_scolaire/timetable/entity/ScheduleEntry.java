package com.ecole.gestion_scolaire.timetable.entity;

import com.ecole.gestion_scolaire.hr.entity.TeachingAssignment;
import com.ecole.gestion_scolaire.school.entity.Room;
import com.ecole.gestion_scolaire.school.entity.TimeSlot;
import com.ecole.gestion_scolaire.timetable.enums.ScheduleEntryStatus;
import com.ecole.gestion_scolaire.timetable.enums.SchoolDay;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "schedule_entry")
public class ScheduleEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teaching_assignment_id", nullable = false)
    private TeachingAssignment teachingAssignment;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 15)
    private SchoolDay dayOfWeek;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "time_slot_id", nullable = false)
    private TimeSlot timeSlot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ScheduleEntryStatus status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        var now = OffsetDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
        if (status == null) status = ScheduleEntryStatus.ACTIVE;
    }

    @PreUpdate
    void preUpdate() { updatedAt = OffsetDateTime.now(); }

    public Long getId() { return id; }
    public TeachingAssignment getTeachingAssignment() { return teachingAssignment; }
    public void setTeachingAssignment(TeachingAssignment v) { teachingAssignment = v; }
    public SchoolDay getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(SchoolDay v) { dayOfWeek = v; }
    public TimeSlot getTimeSlot() { return timeSlot; }
    public void setTimeSlot(TimeSlot v) { timeSlot = v; }
    public Room getRoom() { return room; }
    public void setRoom(Room v) { room = v; }
    public LocalDate getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDate v) { validFrom = v; }
    public LocalDate getValidUntil() { return validUntil; }
    public void setValidUntil(LocalDate v) { validUntil = v; }
    public ScheduleEntryStatus getStatus() { return status; }
    public void setStatus(ScheduleEntryStatus v) { status = v; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
