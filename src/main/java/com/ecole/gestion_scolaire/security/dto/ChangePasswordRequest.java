package com.ecole.gestion_scolaire.security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(

        @NotBlank(message = "Le mot de passe actuel est obligatoire.")
        String currentPassword,

        @NotBlank(message = "Le nouveau mot de passe est obligatoire.")
        @Size(
                min = 12,
                max = 128,
                message = "Le nouveau mot de passe doit contenir entre 12 et 128 caractères."
        )
        String newPassword,

        @NotBlank(message = "La confirmation du mot de passe est obligatoire.")
        String confirmPassword

) {
}