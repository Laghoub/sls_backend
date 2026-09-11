package com.ecole.gestion_scolaire.student.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name="authorized_pickup_person")
public class AuthorizedPickupPerson {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="student_id",nullable=false) private Student student;
    @Column(name="first_name",nullable=false,length=100) private String firstName;
    @Column(name="last_name",nullable=false,length=100) private String lastName;
    @Column(length=100) private String relationship;
    @Column(nullable=false,length=30) private String phone;
    @Column(name="identity_document_number",length=100) private String identityDocumentNumber;
    @Column(name="issued_by",length=150) private String issuedBy;
    @Column(name="valid_from") private LocalDate validFrom;
    @Column(name="valid_until") private LocalDate validUntil;
    @Column(nullable=false) private boolean active;
    public Long getId(){return id;} public Student getStudent(){return student;} public void setStudent(Student v){student=v;}
    public String getFirstName(){return firstName;} public void setFirstName(String v){firstName=v;}
    public String getLastName(){return lastName;} public void setLastName(String v){lastName=v;}
    public String getRelationship(){return relationship;} public void setRelationship(String v){relationship=v;}
    public String getPhone(){return phone;} public void setPhone(String v){phone=v;}
    public String getIdentityDocumentNumber(){return identityDocumentNumber;} public void setIdentityDocumentNumber(String v){identityDocumentNumber=v;}
    public String getIssuedBy(){return issuedBy;} public void setIssuedBy(String v){issuedBy=v;}
    public LocalDate getValidFrom(){return validFrom;} public void setValidFrom(LocalDate v){validFrom=v;}
    public LocalDate getValidUntil(){return validUntil;} public void setValidUntil(LocalDate v){validUntil=v;}
    public boolean isActive(){return active;} public void setActive(boolean v){active=v;}
}
