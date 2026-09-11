package com.ecole.gestion_scolaire.student.dto; import jakarta.validation.constraints.*;
public record GuardianUpdateRequest(@NotBlank @Size(max=30) String status){}
