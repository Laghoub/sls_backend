package com.ecole.gestion_scolaire.student.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record StudentCreateRequest(@NotNull Long personId, @NotBlank @Size(max = 50) String studentNumber,
                                   LocalDate initialAdmissionDate, @NotBlank @Size(max = 30) String status) {
}
