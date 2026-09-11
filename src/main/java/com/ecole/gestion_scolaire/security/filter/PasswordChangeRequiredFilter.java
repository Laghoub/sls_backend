package com.ecole.gestion_scolaire.security.filter;

import com.ecole.gestion_scolaire.security.service.SecurityUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class PasswordChangeRequiredFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    public PasswordChangeRequiredFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof SecurityUser securityUser
                && securityUser.getUserAccount().isMustChangePassword()) {

            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");

            Map<String, Object> body = new LinkedHashMap<>();

            body.put("timestamp", OffsetDateTime.now());
            body.put("status", HttpServletResponse.SC_FORBIDDEN);
            body.put("code", "PASSWORD_CHANGE_REQUIRED");
            body.put(
                    "message",
                    "Vous devez modifier votre mot de passe avant de continuer."
            );
            body.put("path", request.getRequestURI());

            objectMapper.writeValue(
                    response.getOutputStream(),
                    body
            );

            return;
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {

        String path = request.getRequestURI();

        return path.equals("/api/auth/login")
                || path.equals("/api/auth/me")
                || path.equals("/api/auth/change-password")
                || path.equals("/api/auth/logout");
    }
}