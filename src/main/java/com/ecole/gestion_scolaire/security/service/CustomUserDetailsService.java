package com.ecole.gestion_scolaire.security.service;

import com.ecole.gestion_scolaire.identity.entity.Permission;
import com.ecole.gestion_scolaire.identity.entity.Role;
import com.ecole.gestion_scolaire.identity.entity.RolePermission;
import com.ecole.gestion_scolaire.identity.entity.UserAccount;
import com.ecole.gestion_scolaire.identity.entity.UserRole;
import com.ecole.gestion_scolaire.identity.entity.UserAccountStatus;
import com.ecole.gestion_scolaire.identity.repository.RolePermissionRepository;
import com.ecole.gestion_scolaire.identity.repository.UserAccountRepository;
import com.ecole.gestion_scolaire.identity.repository.UserRoleRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserAccountRepository userAccountRepository;
    private final UserRoleRepository userRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;

    public CustomUserDetailsService(
            UserAccountRepository userAccountRepository,
            UserRoleRepository userRoleRepository,
            RolePermissionRepository rolePermissionRepository
    ) {
        this.userAccountRepository = userAccountRepository;
        this.userRoleRepository = userRoleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
    }

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        UserAccount userAccount = userAccountRepository
                .findByUsername(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "Utilisateur introuvable."
                        )
                );

        unlockAccountIfLockExpired(userAccount);

        Set<GrantedAuthority> authorities =
                loadAuthorities(userAccount);

        return new SecurityUser(
                userAccount,
                authorities
        );
    }

    private void unlockAccountIfLockExpired(UserAccount userAccount) {

        if (userAccount.getStatus() != UserAccountStatus.LOCKED) {
            return;
        }

        OffsetDateTime lockedUntil = userAccount.getLockedUntil();

        if (lockedUntil == null) {
            return;
        }

        if (lockedUntil.isAfter(OffsetDateTime.now())) {
            return;
        }

        userAccount.setStatus(UserAccountStatus.ACTIVE);
        userAccount.setFailedLoginAttempts(0);
        userAccount.setLockedUntil(null);

        userAccountRepository.save(userAccount);
    }

    private Set<GrantedAuthority> loadAuthorities(
            UserAccount userAccount
    ) {

        Set<GrantedAuthority> authorities = new HashSet<>();

        List<UserRole> userRoles =
                userRoleRepository.findByUserAccountId(
                        userAccount.getId()
                );

        for (UserRole userRole : userRoles) {

            Role role = userRole.getRole();

            if (!role.isActive()) {
                continue;
            }

            authorities.add(
                    new SimpleGrantedAuthority(
                            "ROLE_" + role.getCode()
                    )
            );

            List<RolePermission> rolePermissions =
                    rolePermissionRepository.findByRoleId(
                            role.getId()
                    );

            for (RolePermission rolePermission : rolePermissions) {

                Permission permission =
                        rolePermission.getPermission();

                if (!permission.isActive()) {
                    continue;
                }

                authorities.add(
                        new SimpleGrantedAuthority(
                                permission.getCode()
                        )
                );
            }
        }

        return authorities;
    }
}