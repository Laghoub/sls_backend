package com.ecole.gestion_scolaire.identity.service;

import com.ecole.gestion_scolaire.identity.dto.access.AccessDtos.*;
import com.ecole.gestion_scolaire.identity.entity.*;
import com.ecole.gestion_scolaire.identity.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class AccessManagementService {
 private final RoleRepository roles; private final PermissionRepository permissions; private final RolePermissionRepository rolePermissions;
 private final UserAccountRepository users; private final UserRoleRepository userRoles; private final UserPermissionOverrideRepository overrides;
 public AccessManagementService(RoleRepository a,PermissionRepository b,RolePermissionRepository c,UserAccountRepository d,UserRoleRepository e,UserPermissionOverrideRepository f){roles=a;permissions=b;rolePermissions=c;users=d;userRoles=e;overrides=f;}

 @Transactional(readOnly=true) public List<PermissionDto> permissions(){return permissions.findAll().stream().sorted(Comparator.comparing(Permission::getModule).thenComparing(Permission::getName)).map(this::permissionDto).toList();}
 @Transactional(readOnly=true) public List<RoleDto> roles(){return roles.findAll().stream().sorted(Comparator.comparing(Role::getName)).map(this::roleDto).toList();}
 @Transactional(readOnly=true) public RoleDto role(Long id){return roleDto(requireRole(id));}

 @Transactional public RoleDto createRole(RoleRequest r){
  String code=normalizeCode(r.code()); if(code.isBlank()) throw new IllegalArgumentException("Le code du profil est obligatoire.");
  if(roles.existsByCode(code)) throw new IllegalArgumentException("Un profil avec ce code existe déjà.");
  Role x=new Role(); x.setCode(code); x.setName(requireText(r.name(),"Le nom du profil est obligatoire.")); x.setDescription(r.description()); x.setSystemRole(false); x.setActive(r.active()==null||r.active());
  return roleDto(roles.save(x));
 }
 @Transactional public RoleDto updateRole(Long id,RoleRequest r){
  Role x=requireRole(id); x.setName(requireText(r.name(),"Le nom du profil est obligatoire.")); x.setDescription(r.description()); if(r.active()!=null)x.setActive(r.active());
  // Le code d'un rôle système reste stable pour préserver la compatibilité des contrôles ROLE_xxx.
  if(!x.isSystemRole()&&r.code()!=null&&!r.code().isBlank()){String code=normalizeCode(r.code()); roles.findByCode(code).filter(o->!Objects.equals(o.getId(),id)).ifPresent(o->{throw new IllegalArgumentException("Ce code de profil est déjà utilisé.");}); x.setCode(code);}
  return roleDto(roles.save(x));
 }
 @Transactional public RoleDto setRolePermissions(Long roleId,PermissionSetRequest req){
  Role role=requireRole(roleId); Set<String> codes=new LinkedHashSet<>(Optional.ofNullable(req.permissionCodes()).orElse(List.of()));
  Map<String,Permission> found=new HashMap<>(); for(Permission p:permissions.findAll())found.put(p.getCode(),p);
  for(String c:codes)if(!found.containsKey(c))throw new IllegalArgumentException("Permission inconnue : "+c);
  rolePermissions.deleteAll(rolePermissions.findByRoleId(roleId)); rolePermissions.flush();
  for(String c:codes){Permission p=found.get(c); if(p.isActive())rolePermissions.save(new RolePermission(role,p));}
  return roleDto(role);
 }
 @Transactional public EffectiveAccessDto setUserRoles(Long userId,UserRolesRequest req){
  UserAccount user=requireUser(userId); List<Long> ids=Optional.ofNullable(req.roleIds()).orElse(List.of());
  userRoles.deleteAll(userRoles.findByUserAccountId(userId)); userRoles.flush();
  for(Long id:new LinkedHashSet<>(ids)){Role r=requireRole(id); if(!r.isActive())throw new IllegalArgumentException("Profil inactif : "+r.getCode()); UserRole ur=new UserRole(); ur.setId(new UserRoleId(user.getId(),r.getId())); ur.setUserAccount(user); ur.setRole(r); userRoles.save(ur);}
  return effective(userId);
 }
 @Transactional public EffectiveAccessDto setOverrides(Long userId,UserOverridesRequest req){
  UserAccount user=requireUser(userId); overrides.deleteAll(overrides.findByUserAccountId(userId)); overrides.flush();
  Map<String,Permission> map=new HashMap<>(); for(Permission p:permissions.findAll())map.put(p.getCode(),p);
  for(OverrideItem i:Optional.ofNullable(req.overrides()).orElse(List.of())){Permission p=map.get(i.permissionCode()); if(p==null)throw new IllegalArgumentException("Permission inconnue : "+i.permissionCode()); String effect=Optional.ofNullable(i.effect()).orElse("").toUpperCase(Locale.ROOT); if(!Set.of("GRANT","DENY").contains(effect))throw new IllegalArgumentException("Effet invalide : "+effect); UserPermissionOverride o=new UserPermissionOverride();o.setUserAccount(user);o.setPermission(p);o.setEffect(effect);overrides.save(o);}
  return effective(userId);
 }
 @Transactional(readOnly=true) public EffectiveAccessDto effective(Long userId){
  requireUser(userId); List<UserRole> urs=userRoles.findByUserAccountId(userId); Set<String> perms=new TreeSet<>(); List<String> roleCodes=new ArrayList<>();
  for(UserRole ur:urs){Role r=ur.getRole();if(!r.isActive())continue;roleCodes.add(r.getCode());for(RolePermission rp:rolePermissions.findByRoleId(r.getId()))if(rp.getPermission().isActive())perms.add(rp.getPermission().getCode());}
  List<OverrideItem> os=new ArrayList<>(); for(UserPermissionOverride o:overrides.findByUserAccountId(userId)){String code=o.getPermission().getCode();os.add(new OverrideItem(code,o.getEffect()));if("DENY".equals(o.getEffect()))perms.remove(code);else if(o.getPermission().isActive())perms.add(code);}
  Collections.sort(roleCodes); os.sort(Comparator.comparing(OverrideItem::permissionCode)); return new EffectiveAccessDto(userId,roleCodes,new ArrayList<>(perms),os);
 }
 private PermissionDto permissionDto(Permission p){return new PermissionDto(p.getId(),p.getCode(),p.getName(),p.getModule(),p.getDescription(),p.isActive());}
 private RoleDto roleDto(Role r){List<String> ps=rolePermissions.findByRoleId(r.getId()).stream().map(x->x.getPermission().getCode()).sorted().toList();return new RoleDto(r.getId(),r.getCode(),r.getName(),r.getDescription(),r.isSystemRole(),r.isActive(),ps);}
 private Role requireRole(Long id){return roles.findById(id).orElseThrow(()->new IllegalArgumentException("Profil introuvable."));}
 private UserAccount requireUser(Long id){return users.findById(id).orElseThrow(()->new IllegalArgumentException("Utilisateur introuvable."));}
 private String normalizeCode(String s){return Optional.ofNullable(s).orElse("").trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+","_").replaceAll("^_|_$","");}
 private String requireText(String s,String msg){if(s==null||s.isBlank())throw new IllegalArgumentException(msg);return s.trim();}
}
