package com.ecole.gestion_scolaire.student.dto; import jakarta.validation.constraints.*;
public record StudentMedicalInfoRequest(@Size(max=10) String bloodGroup,String medicalConditions,String allergies,String treatments,String emergencyNotes){}
