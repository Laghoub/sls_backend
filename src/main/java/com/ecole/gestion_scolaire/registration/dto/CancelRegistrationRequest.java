package com.ecole.gestion_scolaire.registration.dto; import jakarta.validation.constraints.NotBlank; public record CancelRegistrationRequest(@NotBlank String reason){}
