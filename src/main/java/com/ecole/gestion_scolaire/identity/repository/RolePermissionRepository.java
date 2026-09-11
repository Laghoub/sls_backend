package com.ecole.gestion_scolaire.identity.repository;

import com.ecole.gestion_scolaire.identity.entity.RolePermission;
import com.ecole.gestion_scolaire.identity.entity.RolePermissionId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RolePermissionRepository
        extends JpaRepository<RolePermission, RolePermissionId> {

    List<RolePermission> findByRoleId(Long roleId);

    List<RolePermission> findByPermissionId(Long permissionId);
}