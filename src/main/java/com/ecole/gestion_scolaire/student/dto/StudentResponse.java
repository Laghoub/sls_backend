package com.ecole.gestion_scolaire.student.dto; import java.time.*;
public record StudentResponse(Long id,Long personId,String studentNumber,LocalDate initialAdmissionDate,String status,OffsetDateTime createdAt,OffsetDateTime updatedAt){}
