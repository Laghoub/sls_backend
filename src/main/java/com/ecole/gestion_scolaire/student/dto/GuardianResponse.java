package com.ecole.gestion_scolaire.student.dto; import java.time.OffsetDateTime;
public record GuardianResponse(Long id,Long personId,String status,OffsetDateTime createdAt){}
