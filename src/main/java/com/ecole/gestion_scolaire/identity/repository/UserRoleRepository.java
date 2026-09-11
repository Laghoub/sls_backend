package com.ecole.gestion_scolaire.identity.repository;

import com.ecole.gestion_scolaire.identity.entity.UserRole;
import com.ecole.gestion_scolaire.identity.entity.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRoleRepository
        extends JpaRepository<UserRole, UserRoleId> {

    List<UserRole> findByUserAccountId(Long userAccountId);

    List<UserRole> findByRoleId(Long roleId);
}