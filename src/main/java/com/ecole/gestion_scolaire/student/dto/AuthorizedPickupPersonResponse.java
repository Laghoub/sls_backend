package com.ecole.gestion_scolaire.student.dto; import java.time.LocalDate;
public record AuthorizedPickupPersonResponse(Long id,Long studentId,String firstName,String lastName,String relationship,String phone,String identityDocumentNumber,String issuedBy,LocalDate validFrom,LocalDate validUntil,boolean active){}
