package com.ecole.gestion_scolaire.student.dto; import java.time.OffsetDateTime;
public record StudentMedicalInfoResponse(Long id,Long studentId,String bloodGroup,String medicalConditions,String allergies,String treatments,String emergencyNotes,OffsetDateTime updatedAt){}
