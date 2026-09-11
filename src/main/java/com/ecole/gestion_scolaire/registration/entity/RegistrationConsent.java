package com.ecole.gestion_scolaire.registration.entity;
import jakarta.persistence.*; import java.time.OffsetDateTime;
@Entity @Table(name="registration_consent",uniqueConstraints=@UniqueConstraint(name="uq_registration_consent_registration_case_id_consent_type",columnNames={"registration_case_id","consent_type"}))
public class RegistrationConsent {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="registration_case_id",nullable=false) private RegistrationCase registrationCase;
 @Column(name="consent_type",nullable=false,length=50) private String consentType; @Column(nullable=false) private boolean accepted; @Column(name="accepted_at") private OffsetDateTime acceptedAt;
 @Column(name="accepted_by_guardian_id") private Long acceptedByGuardianId; @Column(columnDefinition="text") private String notes;
 public Long getId(){return id;} public RegistrationCase getRegistrationCase(){return registrationCase;} public void setRegistrationCase(RegistrationCase v){registrationCase=v;} public String getConsentType(){return consentType;} public void setConsentType(String v){consentType=v;}
 public boolean isAccepted(){return accepted;} public void setAccepted(boolean v){accepted=v;} public OffsetDateTime getAcceptedAt(){return acceptedAt;} public void setAcceptedAt(OffsetDateTime v){acceptedAt=v;} public Long getAcceptedByGuardianId(){return acceptedByGuardianId;} public void setAcceptedByGuardianId(Long v){acceptedByGuardianId=v;} public String getNotes(){return notes;} public void setNotes(String v){notes=v;}
}
