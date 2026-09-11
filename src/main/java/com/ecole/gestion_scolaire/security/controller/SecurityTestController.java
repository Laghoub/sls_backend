package com.ecole.gestion_scolaire.security.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/security-test")
public class SecurityTestController {

    @GetMapping("/authenticated")
    public Map<String, Object> authenticated() {
        return Map.of(
                "success", true,
                "message", "Utilisateur authentifié."
        );
    }

    @GetMapping("/permission-ok")
    @PreAuthorize("hasAuthority('UTILISATEUR_CONSULTER')")
    public Map<String, Object> permissionOk() {
        return Map.of(
                "success", true,
                "permission", "UTILISATEUR_CONSULTER",
                "message", "Permission accordée."
        );
    }

    @GetMapping("/permission-refused")
    @PreAuthorize("hasAuthority('PERMISSION_TEST_INEXISTANTE')")
    public Map<String, Object> permissionRefused() {
        return Map.of(
                "success", true,
                "message", "Cette réponse ne devrait jamais être accessible."
        );
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> adminOnly() {
        return Map.of(
                "success", true,
                "role", "ADMIN",
                "message", "Accès ADMIN autorisé."
        );
    }
}