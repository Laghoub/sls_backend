package com.ecole.gestion_scolaire.attendance.entity;
import com.ecole.gestion_scolaire.attendance.enums.StudentAttendanceStatus;
import com.ecole.gestion_scolaire.identity.entity.UserAccount;
import com.ecole.gestion_scolaire.student.entity.Student;
import jakarta.persistence.*; import java.time.OffsetDateTime;
@Entity @Table(name="student_attendance") public class StudentAttendance {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="class_session_id",nullable=false) private ClassSession classSession;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="student_id",nullable=false) private Student student;
 @Enumerated(EnumType.STRING) @Column(name="attendance_status",nullable=false,length=30) private StudentAttendanceStatus attendanceStatus;
 @Column(name="late_minutes") private Integer lateMinutes; @Column(columnDefinition="text") private String reason; @Column(nullable=false) private boolean justified;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="recorded_by",nullable=false) private UserAccount recordedBy; @Column(name="recorded_at",nullable=false) private OffsetDateTime recordedAt;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="validated_by") private UserAccount validatedBy; @Column(name="validated_at") private OffsetDateTime validatedAt; @Column(columnDefinition="text") private String notes;
 public Long getId(){return id;} public ClassSession getClassSession(){return classSession;} public void setClassSession(ClassSession v){classSession=v;} public Student getStudent(){return student;} public void setStudent(Student v){student=v;} public StudentAttendanceStatus getAttendanceStatus(){return attendanceStatus;} public void setAttendanceStatus(StudentAttendanceStatus v){attendanceStatus=v;} public Integer getLateMinutes(){return lateMinutes;} public void setLateMinutes(Integer v){lateMinutes=v;} public String getReason(){return reason;} public void setReason(String v){reason=v;} public boolean isJustified(){return justified;} public void setJustified(boolean v){justified=v;} public UserAccount getRecordedBy(){return recordedBy;} public void setRecordedBy(UserAccount v){recordedBy=v;} public OffsetDateTime getRecordedAt(){return recordedAt;} public void setRecordedAt(OffsetDateTime v){recordedAt=v;} public UserAccount getValidatedBy(){return validatedBy;} public void setValidatedBy(UserAccount v){validatedBy=v;} public OffsetDateTime getValidatedAt(){return validatedAt;} public void setValidatedAt(OffsetDateTime v){validatedAt=v;} public String getNotes(){return notes;} public void setNotes(String v){notes=v;} }
