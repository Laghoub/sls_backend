package com.ecole.gestion_scolaire.security.listener;

import com.ecole.gestion_scolaire.identity.entity.UserAccount;
import com.ecole.gestion_scolaire.identity.entity.UserAccountStatus;
import com.ecole.gestion_scolaire.identity.repository.UserAccountRepository;
import com.ecole.gestion_scolaire.security.config.LoginSecurityProperties;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Component
public class AuthenticationFailureListener {

    private final UserAccountRepository userAccountRepository;
    private final LoginSecurityProperties loginSecurityProperties;

    public AuthenticationFailureListener(
            UserAccountRepository userAccountRepository,
            LoginSecurityProperties loginSecurityProperties
    ) {
        this.userAccountRepository = userAccountRepository;
        this.loginSecurityProperties = loginSecurityProperties;
    }

    @EventListener
    @Transactional
    public void onAuthenticationFailure(
            AuthenticationFailureBadCredentialsEvent event
    ) {

        String username = event.getAuthentication().getName();

        if (username == null || username.isBlank()) {
            return;
        }

        userAccountRepository
                .findByUsername(username)
                .ifPresent(this::registerFailedAttempt);
    }

    private void registerFailedAttempt(UserAccount userAccount) {

        if (userAccount.getStatus() != UserAccountStatus.ACTIVE) {
            return;
        }

        int currentAttempts = userAccount.getFailedLoginAttempts();

        int newAttempts = currentAttempts + 1;

        userAccount.setFailedLoginAttempts(newAttempts);

        if (newAttempts >= loginSecurityProperties.getMaxFailedAttempts()) {

            userAccount.setStatus(UserAccountStatus.LOCKED);

            userAccount.setLockedUntil(
                    OffsetDateTime.now().plusMinutes(
                            loginSecurityProperties.getLockDurationMinutes()
                    )
            );
        }

        userAccountRepository.save(userAccount);
    }
}