package com.ecole.gestion_scolaire.identity.controller;

import com.ecole.gestion_scolaire.identity.dto.access.AccessDtos.*;
import com.ecole.gestion_scolaire.identity.service.AccessManagementService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/access-management")
public class AccessManagementController {
 private final AccessManagementService service;
 public AccessManagementController(AccessManagementService service){this.service=service;}
 @GetMapping("/permissions") @PreAuthorize("hasAuthority('PERMISSION_CONSULTER') or hasAuthority('ROLE_GERER')") public List<PermissionDto> permissions(){return service.permissions();}
 @GetMapping("/roles") @PreAuthorize("hasAuthority('ROLE_CONSULTER') or hasAuthority('ROLE_GERER')") public List<RoleDto> roles(){return service.roles();}
 @GetMapping("/roles/{id}") @PreAuthorize("hasAuthority('ROLE_CONSULTER') or hasAuthority('ROLE_GERER')") public RoleDto role(@PathVariable Long id){return service.role(id);}
 @PostMapping("/roles") @PreAuthorize("hasAuthority('ROLE_GERER')") public RoleDto create(@RequestBody RoleRequest r){return service.createRole(r);}
 @PutMapping("/roles/{id}") @PreAuthorize("hasAuthority('ROLE_GERER')") public RoleDto update(@PathVariable Long id,@RequestBody RoleRequest r){return service.updateRole(id,r);}
 @PutMapping("/roles/{id}/permissions") @PreAuthorize("hasAuthority('PERMISSION_GERER') or hasAuthority('PERMISSION_AFFECTER')") public RoleDto permissions(@PathVariable Long id,@RequestBody PermissionSetRequest r){return service.setRolePermissions(id,r);}
 @GetMapping("/users/{userId}/effective") @PreAuthorize("hasAuthority('UTILISATEUR_CONSULTER') or hasAuthority('UTILISATEUR_DROITS_GERER')") public EffectiveAccessDto effective(@PathVariable Long userId){return service.effective(userId);}
 @PutMapping("/users/{userId}/roles") @PreAuthorize("hasAuthority('UTILISATEUR_DROITS_GERER') or hasAuthority('ROLE_AFFECTER')") public EffectiveAccessDto roles(@PathVariable Long userId,@RequestBody UserRolesRequest r){return service.setUserRoles(userId,r);}
 @PutMapping("/users/{userId}/overrides") @PreAuthorize("hasAuthority('UTILISATEUR_DROITS_GERER')") public EffectiveAccessDto overrides(@PathVariable Long userId,@RequestBody UserOverridesRequest r){return service.setOverrides(userId,r);}
}
