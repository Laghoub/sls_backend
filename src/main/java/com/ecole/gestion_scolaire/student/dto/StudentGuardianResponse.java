package com.ecole.gestion_scolaire.student.dto;

import java.time.LocalDate;

public record StudentGuardianResponse(

        Long id,

        Long studentId,

        Long guardianId,

        /*
         * Identité du responsable.
         * Ces informations viennent de Guardian -> Person.
         *
         * Elles sont exposées directement dans le DTO afin
         * d'éviter au frontend de faire plusieurs requêtes
         * supplémentaires.
         */
        String guardianLastName,

        String guardianFirstName,

        String guardianPhone,

        String guardianEmail,

        String relationshipType,

        boolean legalGuardian,

        boolean financialResponsible,

        boolean primaryContact,

        Boolean livesWithStudent,

        boolean pickupAuthorized,

        boolean active,

        LocalDate validFrom,

        LocalDate validUntil

) {
}