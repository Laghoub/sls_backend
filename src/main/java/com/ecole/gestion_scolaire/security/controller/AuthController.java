package com.ecole.gestion_scolaire.security.controller;

import com.ecole.gestion_scolaire.security.dto.AuthUserResponse;
import com.ecole.gestion_scolaire.security.dto.ChangePasswordRequest;
import com.ecole.gestion_scolaire.security.dto.LoginRequest;
import com.ecole.gestion_scolaire.security.dto.LoginResponse;
import com.ecole.gestion_scolaire.security.service.AuthService;
import com.ecole.gestion_scolaire.security.service.SecurityUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final AuthService authService;

    public AuthController(
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository,
            AuthService authService
    ) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {

        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                request.username().trim(),
                                request.password()
                        )
                );

        /*
         * Protection contre la fixation de session.
         *
         * Si une session existe déjà avant le login,
         * son identifiant est changé.
         */
        HttpSession existingSession = httpRequest.getSession(false);

        if (existingSession != null) {
            httpRequest.changeSessionId();
        }

        SecurityContext context =
                SecurityContextHolder.createEmptyContext();

        context.setAuthentication(authentication);

        SecurityContextHolder.setContext(context);

        /*
         * Important :
         * on sauvegarde explicitement l'authentification
         * dans la session HTTP.
         */
        securityContextRepository.saveContext(
                context,
                httpRequest,
                httpResponse
        );

        SecurityUser securityUser =
                (SecurityUser) authentication.getPrincipal();

        AuthUserResponse user =
                authService.handleSuccessfulLogin(securityUser);

        return ResponseEntity.ok(
                new LoginResponse(
                        "Connexion réussie.",
                        user
                )
        );
    }

    @GetMapping("/me")
    public ResponseEntity<AuthUserResponse> me(
            Authentication authentication
    ) {

        SecurityUser securityUser =
                (SecurityUser) authentication.getPrincipal();

        return ResponseEntity.ok(
                authService.buildResponse(securityUser)
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            HttpServletRequest request
    ) {

        HttpSession session = request.getSession(false);

        if (session != null) {
            session.invalidate();
        }

        SecurityContextHolder.clearContext();

        return ResponseEntity.noContent().build();
    }


    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication
    ) {

        SecurityUser securityUser =
                (SecurityUser) authentication.getPrincipal();

        authService.changePassword(
                securityUser,
                request
        );

        return ResponseEntity.noContent().build();
    }




}