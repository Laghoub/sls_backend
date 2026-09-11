package com.ecole.gestion_scolaire.registration.entity;
import jakarta.persistence.*;
@Entity @Table(name="registration_document_requirement",uniqueConstraints=@UniqueConstraint(name="uq_registration_document_requirement_code",columnNames="code"))
public class RegistrationDocumentRequirement {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(nullable=false,length=80) private String code; @Column(nullable=false,length=200) private String name;
 @Column(name="applicable_to",nullable=false,length=50) private String applicableTo; @Column(nullable=false) private boolean required; @Column(nullable=false) private boolean active;
 public Long getId(){return id;} public String getCode(){return code;} public void setCode(String v){code=v;} public String getName(){return name;} public void setName(String v){name=v;} public String getApplicableTo(){return applicableTo;} public void setApplicableTo(String v){applicableTo=v;} public boolean isRequired(){return required;} public void setRequired(boolean v){required=v;} public boolean isActive(){return active;} public void setActive(boolean v){active=v;}
}
