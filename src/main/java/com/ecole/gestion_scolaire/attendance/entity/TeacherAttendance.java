package com.ecole.gestion_scolaire.attendance.entity;

import com.ecole.gestion_scolaire.attendance.enums.AttendanceValidationStatus;
import com.ecole.gestion_scolaire.attendance.enums.TeacherAttendanceStatus;
import com.ecole.gestion_scolaire.hr.entity.Teacher;
import com.ecole.gestion_scolaire.identity.entity.UserAccount;
import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "teacher_attendance")
public class TeacherAttendance {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_session_id", nullable = false)
    private ClassSession classSession;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @Enumerated(EnumType.STRING)
    @Column(name = "attendance_status", nullable = false, length = 30)
    private TeacherAttendanceStatus attendanceStatus;

    @Column(name = "late_minutes")
    private Integer lateMinutes;

    @Column(name = "reason")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "validation_status", nullable = false, length = 30)
    private AttendanceValidationStatus validationStatus;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recorded_by", nullable = false)
    private UserAccount recordedBy;

    @Column(name = "recorded_at", nullable = false)
    private OffsetDateTime recordedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validated_by")
    private UserAccount validatedBy;

    @Column(name = "validated_at")
    private OffsetDateTime validatedAt;

    @Column(name = "notes")
    private String notes;

    @PrePersist void prePersist() {
        if (recordedAt == null) recordedAt = OffsetDateTime.now();
        if (validationStatus == null) validationStatus = AttendanceValidationStatus.VALIDATED;
    }

    public Long getId() { return id; }
    public ClassSession getClassSession() { return classSession; }
    public void setClassSession(ClassSession v) { classSession = v; }
    public Teacher getTeacher() { return teacher; }
    public void setTeacher(Teacher v) { teacher = v; }
    public TeacherAttendanceStatus getAttendanceStatus() { return attendanceStatus; }
    public void setAttendanceStatus(TeacherAttendanceStatus v) { attendanceStatus = v; }
    public Integer getLateMinutes() { return lateMinutes; }
    public void setLateMinutes(Integer v) { lateMinutes = v; }
    public String getReason() { return reason; }
    public void setReason(String v) { reason = v; }
    public AttendanceValidationStatus getValidationStatus() { return validationStatus; }
    public void setValidationStatus(AttendanceValidationStatus v) { validationStatus = v; }
    public UserAccount getRecordedBy() { return recordedBy; }
    public void setRecordedBy(UserAccount v) { recordedBy = v; }
    public OffsetDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(OffsetDateTime v) { recordedAt = v; }
    public UserAccount getValidatedBy() { return validatedBy; }
    public void setValidatedBy(UserAccount v) { validatedBy = v; }
    public OffsetDateTime getValidatedAt() { return validatedAt; }
    public void setValidatedAt(OffsetDateTime v) { validatedAt = v; }
    public String getNotes() { return notes; }
    public void setNotes(String v) { notes = v; }
}
