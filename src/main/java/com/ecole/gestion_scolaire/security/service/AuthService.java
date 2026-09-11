package com.ecole.gestion_scolaire.security.service;

import com.ecole.gestion_scolaire.identity.entity.UserAccount;
import com.ecole.gestion_scolaire.identity.repository.UserAccountRepository;
import com.ecole.gestion_scolaire.security.dto.AuthUserResponse;
import com.ecole.gestion_scolaire.security.dto.ChangePasswordRequest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Service
public class AuthService {

    private static final String ROLE_PREFIX = "ROLE_";

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AuthUserResponse handleSuccessfulLogin(SecurityUser securityUser) {

        UserAccount userAccount = securityUser.getUserAccount();

        userAccount.setLastLoginAt(OffsetDateTime.now());
        userAccount.setFailedLoginAttempts(0);

        userAccountRepository.save(userAccount);

        return buildResponse(securityUser);
    }

    @Transactional
    public void changePassword(
            SecurityUser securityUser,
            ChangePasswordRequest request
    ) {

        UserAccount userAccount = userAccountRepository
                .findById(securityUser.getId())
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Compte utilisateur introuvable."
                        )
                );

        if (!passwordEncoder.matches(
                request.currentPassword(),
                userAccount.getPasswordHash()
        )) {
            throw new IllegalArgumentException(
                    "Le mot de passe actuel est incorrect."
            );
        }

        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new IllegalArgumentException(
                    "La confirmation du nouveau mot de passe ne correspond pas."
            );
        }

        if (passwordEncoder.matches(
                request.newPassword(),
                userAccount.getPasswordHash()
        )) {
            throw new IllegalArgumentException(
                    "Le nouveau mot de passe doit être différent du mot de passe actuel."
            );
        }

        userAccount.setPasswordHash(
                passwordEncoder.encode(request.newPassword())
        );

        userAccount.setMustChangePassword(false);
        userAccount.setPasswordChangedAt(OffsetDateTime.now());

        userAccountRepository.save(userAccount);
    }

    public AuthUserResponse buildResponse(SecurityUser securityUser) {

        Set<String> roles = new LinkedHashSet<>();
        Set<String> permissions = new LinkedHashSet<>();

        for (GrantedAuthority authority : securityUser.getAuthorities()) {

            String value = authority.getAuthority();

            if (value.startsWith(ROLE_PREFIX)) {
                roles.add(value.substring(ROLE_PREFIX.length()));
            } else {
                permissions.add(value);
            }
        }

        return new AuthUserResponse(
                securityUser.getId(),
                securityUser.getUsername(),
                securityUser.getUserAccount().isMustChangePassword(),
                roles,
                permissions
        );
    }
}