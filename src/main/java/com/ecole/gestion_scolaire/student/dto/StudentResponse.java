package com.ecole.gestion_scolaire.student.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record StudentResponse(
        Long id, Long personId, String studentNumber,
        LocalDate initialAdmissionDate, String status,
        OffsetDateTime createdAt, OffsetDateTime updatedAt,
        String firstName, String lastName
) {
    // Maintien du constructeur historique pour la compatibilité des appelants.
    public StudentResponse(Long id, Long personId, String studentNumber,
                           LocalDate initialAdmissionDate, String status,
                           OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this(id, personId, studentNumber, initialAdmissionDate, status, createdAt, updatedAt, null, null);
    }
}
