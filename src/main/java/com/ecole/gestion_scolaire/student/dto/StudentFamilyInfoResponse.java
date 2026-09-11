package com.ecole.gestion_scolaire.student.dto; import java.time.OffsetDateTime;
public record StudentFamilyInfoResponse(Long id,Long studentId,String fatherLifeStatus,String motherLifeStatus,Boolean parentsDivorced,Integer numberOfBrothers,Integer numberOfSisters,String notes,OffsetDateTime updatedAt){}
