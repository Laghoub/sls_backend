package com.ecole.gestion_scolaire.security.dto;

public record LoginResponse(

        String message,
        AuthUserResponse user

) {
}