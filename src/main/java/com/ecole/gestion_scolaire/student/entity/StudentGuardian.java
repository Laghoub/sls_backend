package com.ecole.gestion_scolaire.student.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name="student_guardian", uniqueConstraints=@UniqueConstraint(name="uq_student_guardian_student_id_guardian_id", columnNames={"student_id","guardian_id"}))
public class StudentGuardian {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="student_id",nullable=false) private Student student;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="guardian_id",nullable=false) private Guardian guardian;
    @Column(name="relationship_type",nullable=false,length=30) private String relationshipType;
    @Column(name="legal_guardian",nullable=false) private boolean legalGuardian;
    @Column(name="financial_responsible",nullable=false) private boolean financialResponsible;
    @Column(name="primary_contact",nullable=false) private boolean primaryContact;
    @Column(name="lives_with_student") private Boolean livesWithStudent;
    @Column(name="pickup_authorized",nullable=false) private boolean pickupAuthorized;
    @Column(nullable=false) private boolean active;
    @Column(name="valid_from") private LocalDate validFrom;
    @Column(name="valid_until") private LocalDate validUntil;
    public Long getId(){return id;} public void setId(Long v){id=v;}
    public Student getStudent(){return student;} public void setStudent(Student v){student=v;}
    public Guardian getGuardian(){return guardian;} public void setGuardian(Guardian v){guardian=v;}
    public String getRelationshipType(){return relationshipType;} public void setRelationshipType(String v){relationshipType=v;}
    public boolean isLegalGuardian(){return legalGuardian;} public void setLegalGuardian(boolean v){legalGuardian=v;}
    public boolean isFinancialResponsible(){return financialResponsible;} public void setFinancialResponsible(boolean v){financialResponsible=v;}
    public boolean isPrimaryContact(){return primaryContact;} public void setPrimaryContact(boolean v){primaryContact=v;}
    public Boolean getLivesWithStudent(){return livesWithStudent;} public void setLivesWithStudent(Boolean v){livesWithStudent=v;}
    public boolean isPickupAuthorized(){return pickupAuthorized;} public void setPickupAuthorized(boolean v){pickupAuthorized=v;}
    public boolean isActive(){return active;} public void setActive(boolean v){active=v;}
    public LocalDate getValidFrom(){return validFrom;} public void setValidFrom(LocalDate v){validFrom=v;}
    public LocalDate getValidUntil(){return validUntil;} public void setValidUntil(LocalDate v){validUntil=v;}
}
