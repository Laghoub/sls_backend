package com.ecole.gestion_scolaire.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.security.login")
public class LoginSecurityProperties {

    private int maxFailedAttempts = 5;
    private long lockDurationMinutes = 15;

    public int getMaxFailedAttempts() {
        return maxFailedAttempts;
    }

    public void setMaxFailedAttempts(int maxFailedAttempts) {
        this.maxFailedAttempts = maxFailedAttempts;
    }

    public long getLockDurationMinutes() {
        return lockDurationMinutes;
    }

    public void setLockDurationMinutes(long lockDurationMinutes) {
        this.lockDurationMinutes = lockDurationMinutes;
    }
}