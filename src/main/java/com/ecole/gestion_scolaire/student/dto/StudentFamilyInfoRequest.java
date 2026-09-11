package com.ecole.gestion_scolaire.student.dto; import jakarta.validation.constraints.*;
public record StudentFamilyInfoRequest(@Size(max=20) String fatherLifeStatus,@Size(max=20) String motherLifeStatus,Boolean parentsDivorced,@PositiveOrZero Integer numberOfBrothers,@PositiveOrZero Integer numberOfSisters,String notes){}
