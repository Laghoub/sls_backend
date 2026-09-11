package com.ecole.gestion_scolaire.identity.repository;

import com.ecole.gestion_scolaire.identity.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByUsername(String username);

    boolean existsByUsername(String username);

    Optional<UserAccount> findByPersonId(Long personId);
}