package com.ecole.gestion_scolaire.identity.repository;

import com.ecole.gestion_scolaire.identity.entity.UserScope;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserScopeRepository extends JpaRepository<UserScope, Long> {

    List<UserScope> findByUserAccountId(Long userAccountId);

    List<UserScope> findByUserAccountIdAndScopeType(
            Long userAccountId,
            String scopeType
    );
}