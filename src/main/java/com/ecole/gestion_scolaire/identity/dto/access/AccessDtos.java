package com.ecole.gestion_scolaire.identity.dto.access;
import java.util.List;
public final class AccessDtos {
 private AccessDtos(){}
 public record PermissionDto(Long id,String code,String name,String module,String description,boolean active){}
 public record RoleDto(Long id,String code,String name,String description,boolean systemRole,boolean active,List<String> permissions){}
 public record RoleRequest(String code,String name,String description,Boolean active){}
 public record PermissionSetRequest(List<String> permissionCodes){}
 public record UserRolesRequest(List<Long> roleIds){}
 public record OverrideItem(String permissionCode,String effect){}
 public record UserOverridesRequest(List<OverrideItem> overrides){}
 public record EffectiveAccessDto(Long userAccountId,List<String> roles,List<String> permissions,List<OverrideItem> overrides){}
}
