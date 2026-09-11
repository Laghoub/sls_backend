package com.ecole.gestion_scolaire.student.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name="student_medical_info", uniqueConstraints=@UniqueConstraint(name="uq_student_medical_info_student_id", columnNames="student_id"))
public class StudentMedicalInfo {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @OneToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="student_id",nullable=false) private Student student;
    @Column(name="blood_group",length=10) private String bloodGroup;
    @Column(name="medical_conditions",columnDefinition="TEXT") private String medicalConditions;
    @Column(columnDefinition="TEXT") private String allergies;
    @Column(columnDefinition="TEXT") private String treatments;
    @Column(name="emergency_notes",columnDefinition="TEXT") private String emergencyNotes;
    @Column(name="updated_at",nullable=false) private OffsetDateTime updatedAt;
    @PrePersist @PreUpdate void touch(){updatedAt=OffsetDateTime.now();}
    public Long getId(){return id;} public Student getStudent(){return student;} public void setStudent(Student v){student=v;}
    public String getBloodGroup(){return bloodGroup;} public void setBloodGroup(String v){bloodGroup=v;}
    public String getMedicalConditions(){return medicalConditions;} public void setMedicalConditions(String v){medicalConditions=v;}
    public String getAllergies(){return allergies;} public void setAllergies(String v){allergies=v;}
    public String getTreatments(){return treatments;} public void setTreatments(String v){treatments=v;}
    public String getEmergencyNotes(){return emergencyNotes;} public void setEmergencyNotes(String v){emergencyNotes=v;}
    public OffsetDateTime getUpdatedAt(){return updatedAt;}
}
