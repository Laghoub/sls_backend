package com.ecole.gestion_scolaire.bootstrap;

import com.ecole.gestion_scolaire.identity.entity.Person;
import com.ecole.gestion_scolaire.identity.entity.Role;
import com.ecole.gestion_scolaire.identity.entity.UserAccount;
import com.ecole.gestion_scolaire.identity.entity.UserAccountStatus;
import com.ecole.gestion_scolaire.identity.entity.UserRole;
import com.ecole.gestion_scolaire.identity.entity.UserRoleId;
import com.ecole.gestion_scolaire.identity.repository.PersonRepository;
import com.ecole.gestion_scolaire.identity.repository.RoleRepository;
import com.ecole.gestion_scolaire.identity.repository.UserAccountRepository;
import com.ecole.gestion_scolaire.identity.repository.UserRoleRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Component
public class BootstrapAdminInitializer implements ApplicationRunner {

    private final PersonRepository personRepository;
    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap-admin.enabled:false}")
    private boolean enabled;

    @Value("${app.bootstrap-admin.username:}")
    private String username;

    @Value("${app.bootstrap-admin.password:}")
    private String password;

    @Value("${app.bootstrap-admin.first-name:}")
    private String firstName;

    @Value("${app.bootstrap-admin.last-name:}")
    private String lastName;

    @Value("${app.bootstrap-admin.email:}")
    private String email;

    public BootstrapAdminInitializer(
            PersonRepository personRepository,
            UserAccountRepository userAccountRepository,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.personRepository = personRepository;
        this.userAccountRepository = userAccountRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {

        if (!enabled) {
            return;
        }

        validateConfiguration();

        if (userAccountRepository.existsByUsername(username)) {
            System.out.println(
                    "Bootstrap ADMIN ignoré : le nom d'utilisateur existe déjà."
            );
            return;
        }

        Role adminRole = roleRepository
                .findByCode("ADMIN")
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Le rôle ADMIN est introuvable."
                        )
                );

        Person person = new Person();

        person.setFirstName(firstName.trim());
        person.setLastName(lastName.trim());

        if (email != null && !email.isBlank()) {
            person.setEmail(email.trim());
        }

        person = personRepository.save(person);

        UserAccount userAccount = new UserAccount();

        userAccount.setPerson(person);
        userAccount.setUsername(username.trim());
        userAccount.setPasswordHash(passwordEncoder.encode(password));
        userAccount.setStatus(UserAccountStatus.ACTIVE);
        userAccount.setMustChangePassword(true);
        userAccount.setFailedLoginAttempts(0);
        userAccount.setPasswordChangedAt(OffsetDateTime.now());

        userAccount = userAccountRepository.save(userAccount);

        UserRole userRole = new UserRole();

        userRole.setId(
                new UserRoleId(
                        userAccount.getId(),
                        adminRole.getId()
                )
        );

        userRole.setUserAccount(userAccount);
        userRole.setRole(adminRole);
        userRole.setAssignedAt(OffsetDateTime.now());

        /*
         * Il s'agit du tout premier administrateur.
         * Il n'existe donc pas encore d'utilisateur pouvant être
         * renseigné dans assigned_by.
         */
        userRole.setAssignedBy(null);

        userRoleRepository.save(userRole);

        System.out.println(
                "Premier compte ADMIN créé avec succès."
        );
    }

    private void validateConfiguration() {

        if (username == null || username.isBlank()) {
            throw new IllegalStateException(
                    "APP_BOOTSTRAP_ADMIN_USERNAME est obligatoire."
            );
        }

        if (password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "APP_BOOTSTRAP_ADMIN_PASSWORD est obligatoire."
            );
        }

        if (password.length() < 12) {
            throw new IllegalStateException(
                    "Le mot de passe bootstrap ADMIN doit contenir au moins 12 caractères."
            );
        }

        if (firstName == null || firstName.isBlank()) {
            throw new IllegalStateException(
                    "APP_BOOTSTRAP_ADMIN_FIRST_NAME est obligatoire."
            );
        }

        if (lastName == null || lastName.isBlank()) {
            throw new IllegalStateException(
                    "APP_BOOTSTRAP_ADMIN_LAST_NAME est obligatoire."
            );
        }
    }
}