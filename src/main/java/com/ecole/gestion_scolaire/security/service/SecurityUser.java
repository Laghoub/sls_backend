package com.ecole.gestion_scolaire.security.service;

import com.ecole.gestion_scolaire.identity.entity.UserAccount;
import com.ecole.gestion_scolaire.identity.entity.UserAccountStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Set;

public class SecurityUser implements UserDetails {

    private final UserAccount userAccount;
    private final Set<GrantedAuthority> authorities;

    public SecurityUser(
            UserAccount userAccount,
            Set<GrantedAuthority> authorities
    ) {
        this.userAccount = userAccount;
        this.authorities = authorities;
    }

    public Long getId() {
        return userAccount.getId();
    }

    public UserAccount getUserAccount() {
        return userAccount;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return userAccount.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return userAccount.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return userAccount.getStatus()
                != UserAccountStatus.ARCHIVED;
    }

    @Override
    public boolean isAccountNonLocked() {

        if (userAccount.getStatus()
                != UserAccountStatus.LOCKED) {
            return true;
        }

        OffsetDateTime lockedUntil =
                userAccount.getLockedUntil();

        if (lockedUntil == null) {
            return false;
        }

        return lockedUntil.isBefore(
                OffsetDateTime.now()
        );
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return userAccount.getStatus()
                == UserAccountStatus.ACTIVE;
    }
}