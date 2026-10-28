package com.ecole.gestion_scolaire.identity.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import java.util.Set;

/** Centralise le périmètre des surveillants. Ne remplace pas les contrôles d'autorisation des endpoints. */
@Component
public class SurveillantCycleScope {
    public String restrictedCycle() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken) {
            throw new AccessDeniedException("Authentification requise");
        }
        Set<String> roles = authentication.getAuthorities().stream()
                .map(org.springframework.security.core.GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .collect(java.util.stream.Collectors.toSet());

        // Les autorités ROLE_* sont bien émises par CustomUserDetailsService.
        // Un profil de surveillance inconnu ou plusieurs cycles incompatibles
        // ne doivent jamais se traduire par un accès global.
        boolean surveillance = roles.stream().anyMatch(role -> role.startsWith("ROLE_SURVEILLANT"));
        if (!surveillance) return null; // Autres profils : règles existantes inchangées.
        if (roles.contains("ROLE_ADMIN") || roles.contains("ROLE_SURVEILLANT_GENERAL")) return null;

        Set<String> cycles = new java.util.HashSet<>();
        if (roles.contains("ROLE_SURVEILLANT_LYCEE")) cycles.add("LY");
        if (roles.contains("ROLE_SURVEILLANT_COLLEGE")) cycles.add("CO");
        if (roles.contains("ROLE_SURVEILLANT_PRIMAIRE")) cycles.add("PR");
        if (cycles.size() != 1 || roles.stream().anyMatch(role ->
                role.startsWith("ROLE_SURVEILLANT_") && !Set.of(
                        "ROLE_SURVEILLANT_LYCEE", "ROLE_SURVEILLANT_COLLEGE",
                        "ROLE_SURVEILLANT_PRIMAIRE", "ROLE_SURVEILLANT_GENERAL"
                ).contains(role))) {
            throw new AccessDeniedException("Profil de surveillance ambigu ou non reconnu");
        }
        return cycles.iterator().next();
    }
    public void assertCycle(String cycle) {
        String required = restrictedCycle();
        if (required != null && !required.equalsIgnoreCase(cycle))
            throw new AccessDeniedException("Cycle scolaire non autorisé");
    }
}
