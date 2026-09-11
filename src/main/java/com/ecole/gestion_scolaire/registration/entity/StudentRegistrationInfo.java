package com.ecole.gestion_scolaire.registration.entity;
import jakarta.persistence.*;
@Entity @Table(name="student_registration_info",uniqueConstraints=@UniqueConstraint(name="uq_student_registration_info_registration_case_id",columnNames="registration_case_id"))
public class StudentRegistrationInfo {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @OneToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="registration_case_id",nullable=false) private RegistrationCase registrationCase;
 @Column(name="previous_school",length=200) private String previousSchool; @Column(name="previous_class",length=100) private String previousClass; @Column(columnDefinition="text") private String notes;
 public Long getId(){return id;} public RegistrationCase getRegistrationCase(){return registrationCase;} public void setRegistrationCase(RegistrationCase v){registrationCase=v;}
 public String getPreviousSchool(){return previousSchool;} public void setPreviousSchool(String v){previousSchool=v;} public String getPreviousClass(){return previousClass;} public void setPreviousClass(String v){previousClass=v;} public String getNotes(){return notes;} public void setNotes(String v){notes=v;}
}
