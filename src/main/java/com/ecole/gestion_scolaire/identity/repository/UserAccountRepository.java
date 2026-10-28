package com.ecole.gestion_scolaire.identity.repository;

import com.ecole.gestion_scolaire.identity.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    @EntityGraph(attributePaths = "person")
    Optional<UserAccount> findByUsername(String username);

    @EntityGraph(attributePaths = "person")
    Optional<UserAccount> findOneById(Long id);

    boolean existsByUsername(String username);

    @EntityGraph(attributePaths = "person")
    Optional<UserAccount> findByPersonId(Long personId);
}