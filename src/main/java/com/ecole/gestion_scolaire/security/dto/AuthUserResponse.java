package com.ecole.gestion_scolaire.security.dto;

import java.util.Set;

public record AuthUserResponse(

        Long id,
        String username,
        boolean mustChangePassword,
        Set<String> roles,
        Set<String> permissions

) {
}