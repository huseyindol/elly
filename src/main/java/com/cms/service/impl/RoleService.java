package com.cms.service.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.config.TenantContext;
import com.cms.config.UserAuthCacheService;
import com.cms.dto.DtoPermission;
import com.cms.dto.DtoRole;
import com.cms.dto.DtoRoleIU;
import com.cms.entity.Permission;
import com.cms.entity.Role;
import com.cms.entity.User;
import com.cms.exception.ConflictException;
import com.cms.exception.ResourceNotFoundException;
import com.cms.repository.PermissionRepository;
import com.cms.repository.RoleRepository;
import com.cms.repository.UserRepository;
import com.cms.service.IRoleService;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;

@Service
@RequiredArgsConstructor
public class RoleService implements IRoleService {

  private final RoleRepository roleRepository;
  private final PermissionRepository permissionRepository;
  private final UserRepository userRepository;
  private final UserAuthCacheService userAuthCacheService;

  @Value("${app.tenants.default-tenant:basedb}")
  private String defaultTenant;

  @Override
  public List<DtoRole> getAllRoles() {
    return executeInDefaultTenant(() -> {
      List<Role> roles = roleRepository.findAll();
      return roles.stream().map(this::toDto).collect(Collectors.toList());
    });
  }

  @Override
  public DtoRole getRoleById(Long id) {
    return executeInDefaultTenant(() -> {
      Role role = roleRepository.findById(id)
          .orElseThrow(() -> new ResourceNotFoundException("Role", "id", id));
      return toDto(role);
    });
  }

  @Override
  @Transactional
  public DtoRole createRole(DtoRoleIU dto) {
    return executeInDefaultTenant(() -> {
      if (roleRepository.existsByName(dto.getName())) {
        throw new ConflictException("Role already exists: " + dto.getName());
      }
      Role role = new Role();
      role.setName(dto.getName());
      role.setDescription(dto.getDescription());
      Role saved = roleRepository.save(role);
      return toDto(saved);
    });
  }

  @Override
  @Transactional
  public DtoRole updateRole(Long id, DtoRoleIU dto) {
    return executeInDefaultTenant(() -> {
      Role role = roleRepository.findById(id)
          .orElseThrow(() -> new ResourceNotFoundException("Role", "id", id));
      role.setName(dto.getName());
      role.setDescription(dto.getDescription());
      Role saved = roleRepository.save(role);
      return toDto(saved);
    });
  }

  @Override
  @Transactional
  public Boolean deleteRole(Long id) {
    return executeInDefaultTenant(() -> {
      Role role = roleRepository.findById(id)
          .orElseThrow(() -> new ResourceNotFoundException("Role", "id", id));
      roleRepository.delete(role);
      return true;
    });
  }

  @Override
  @Transactional
  public DtoRole assignPermissionsToRole(Long roleId, Set<Long> permissionIds) {
    return executeInDefaultTenant(() -> {
      Role role = roleRepository.findById(roleId)
          .orElseThrow(() -> new ResourceNotFoundException("Role", "id", roleId));
      Set<Permission> permissions = new HashSet<>(permissionRepository.findAllById(permissionIds));
      role.setPermissions(permissions);
      Role saved = roleRepository.save(role);
      return toDto(saved);
    });
  }

  @Override
  @Transactional
  public void assignRolesToUser(Long userId, Set<Long> roleIds) {
    executeInDefaultTenant(() -> {
      User user = userRepository.findById(userId)
          .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
      Set<Role> roles = new HashSet<>(roleRepository.findAllById(roleIds));
      user.setRoles(roles);
      userRepository.save(user);
      // Kullanıcının auth cache'ini temizle — yeni roller hemen uygulanır
      userAuthCacheService.evictUserCache(user.getUsername());
      return null;
    });
  }

  @Override
  public List<DtoPermission> getAllPermissions() {
    return executeInDefaultTenant(() -> {
      List<Permission> permissions = permissionRepository.findAll();
      return permissions.stream().map(this::toPermissionDto).collect(Collectors.toList());
    });
  }

  @Override
  public List<DtoPermission> getPermissionsByModule(String module) {
    return executeInDefaultTenant(() -> {
      List<Permission> permissions = permissionRepository.findByModule(module);
      return permissions.stream().map(this::toPermissionDto).collect(Collectors.toList());
    });
  }

  // =============== Helpers ===============

  /**
   * Role/Permission işlemleri her zaman defaultTenant (basedb) üzerinde yapılır
   * çünkü User entity'si orada tutulur.
   */
  private <T> T executeInDefaultTenant(java.util.function.Supplier<T> action) {
    String originalTenant = TenantContext.getTenantId();
    try {
      TenantContext.setTenantId(defaultTenant);
      return action.get();
    } finally {
      TenantContext.setTenantId(originalTenant);
    }
  }

  private DtoRole toDto(Role role) {
    DtoRole dto = new DtoRole();
    dto.setId(role.getId());
    dto.setName(role.getName());
    dto.setDescription(role.getDescription());
    if (role.getPermissions() != null) {
      dto.setPermissions(role.getPermissions().stream()
          .map(this::toPermissionDto)
          .collect(Collectors.toSet()));
    }
    return dto;
  }

  private DtoPermission toPermissionDto(Permission permission) {
    DtoPermission dto = new DtoPermission();
    dto.setId(permission.getId());
    dto.setName(permission.getName());
    dto.setDescription(permission.getDescription());
    dto.setModule(permission.getModule());
    return dto;
  }
}
