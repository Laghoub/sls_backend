package com.ecole.gestion_scolaire.registration.entity;
import com.ecole.gestion_scolaire.registration.enums.RegistrationStatus;
import com.ecole.gestion_scolaire.student.entity.Guardian;
import com.ecole.gestion_scolaire.student.entity.Student;
import jakarta.persistence.*;
import java.time.*;
@Entity @Table(name="registration_case", uniqueConstraints=@UniqueConstraint(name="uq_registration_case_student_id_school_year_id",columnNames={"student_id","school_year_id"}))
public class RegistrationCase {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="school_year_id",nullable=false) private Long schoolYearId;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="student_id",nullable=false) private Student student;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="guardian_id",nullable=false) private Guardian guardian;
 @Column(name="requested_level_id",nullable=false) private Long requestedLevelId;
 @Column(name="requested_class_group_id") private Long requestedClassGroupId;
 @Column(name="registration_date",nullable=false) private LocalDate registrationDate;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=40) private RegistrationStatus status;
 @Column(name="created_by",nullable=false) private Long createdById;
 @Column(name="finalized_at") private OffsetDateTime finalizedAt;
 @Column(name="cancelled_at") private OffsetDateTime cancelledAt;
 @Column(name="cancellation_reason") private String cancellationReason;
 @Column(name="created_at",nullable=false) private OffsetDateTime createdAt;
 @Column(name="updated_at",nullable=false) private OffsetDateTime updatedAt;
 @PrePersist void prePersist(){var n=OffsetDateTime.now(); if(createdAt==null)createdAt=n; updatedAt=n; if(registrationDate==null)registrationDate=LocalDate.now(); if(status==null)status=RegistrationStatus.PRE_INSCRIPTION;}
 @PreUpdate void preUpdate(){updatedAt=OffsetDateTime.now();}
 public Long getId(){return id;} public Long getSchoolYearId(){return schoolYearId;} public void setSchoolYearId(Long v){schoolYearId=v;}
 public Student getStudent(){return student;} public void setStudent(Student v){student=v;} public Guardian getGuardian(){return guardian;} public void setGuardian(Guardian v){guardian=v;}
 public Long getRequestedLevelId(){return requestedLevelId;} public void setRequestedLevelId(Long v){requestedLevelId=v;} public Long getRequestedClassGroupId(){return requestedClassGroupId;} public void setRequestedClassGroupId(Long v){requestedClassGroupId=v;}
 public LocalDate getRegistrationDate(){return registrationDate;} public void setRegistrationDate(LocalDate v){registrationDate=v;} public RegistrationStatus getStatus(){return status;} public void setStatus(RegistrationStatus v){status=v;}
 public Long getCreatedById(){return createdById;} public void setCreatedById(Long v){createdById=v;} public OffsetDateTime getFinalizedAt(){return finalizedAt;} public void setFinalizedAt(OffsetDateTime v){finalizedAt=v;}
 public OffsetDateTime getCancelledAt(){return cancelledAt;} public void setCancelledAt(OffsetDateTime v){cancelledAt=v;} public String getCancellationReason(){return cancellationReason;} public void setCancellationReason(String v){cancellationReason=v;}
 public OffsetDateTime getCreatedAt(){return createdAt;} public OffsetDateTime getUpdatedAt(){return updatedAt;}
}
