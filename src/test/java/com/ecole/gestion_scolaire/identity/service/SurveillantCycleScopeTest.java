package com.ecole.gestion_scolaire.identity.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

class SurveillantCycleScopeTest {
    private final SurveillantCycleScope scope = new SurveillantCycleScope();
    private void login(String... roles) {
        var authorities = Arrays.stream(roles).map(SimpleGrantedAuthority::new).toList();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("test", "unused", authorities));
    }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    @Test void lyceeCannotAccessCollege() {
        login("ROLE_SURVEILLANT_LYCEE");
        assertEquals("LYCEE", scope.restrictedCycle());
        assertThrows(AccessDeniedException.class, () -> scope.assertCycle("COLLEGE"));
        assertDoesNotThrow(() -> scope.assertCycle("LYCEE"));
    }
    @Test void primaireAndCollegeAreSeparated() {
        login("ROLE_SURVEILLANT_PRIMAIRE");
        assertThrows(AccessDeniedException.class, () -> scope.assertCycle("COLLEGE"));
        login("ROLE_SURVEILLANT_COLLEGE");
        assertThrows(AccessDeniedException.class, () -> scope.assertCycle("PRIMAIRE"));
    }
    @Test void ambiguousSurveillanceIsDenied() {
        login("ROLE_SURVEILLANT_LYCEE", "ROLE_SURVEILLANT_COLLEGE");
        assertThrows(AccessDeniedException.class, scope::restrictedCycle);
    }
    @Test void unknownSurveillanceIsDenied() {
        login("ROLE_SURVEILLANT_INCONNU");
        assertThrows(AccessDeniedException.class, scope::restrictedCycle);
    }
    @Test void generalAndAdminRetainGlobalScope() {
        login("ROLE_SURVEILLANT_GENERAL");
        assertNull(scope.restrictedCycle());
        login("ROLE_ADMIN");
        assertNull(scope.restrictedCycle());
    }
    @Test void unauthenticatedIsDenied() {
        SecurityContextHolder.clearContext();
        assertThrows(AccessDeniedException.class, scope::restrictedCycle);
    }
}
