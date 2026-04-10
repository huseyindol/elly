package com.cms.controller.impl;

import java.util.List;
import java.util.Set;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cms.controller.IRoleController;
import com.cms.dto.DtoPermission;
import com.cms.dto.DtoRole;
import com.cms.dto.DtoRoleIU;
import com.cms.dto.DtoUserRoleAssignment;
import com.cms.entity.RootEntityResponse;
import com.cms.service.IRoleService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RoleController extends BaseController implements IRoleController {

  private final IRoleService roleService;

  @Override
  @GetMapping
  @PreAuthorize("hasAuthority('roles:read')")
  public RootEntityResponse<List<DtoRole>> getAllRoles() {
    return ok(roleService.getAllRoles());
  }

  @Override
  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('roles:read')")
  public RootEntityResponse<DtoRole> getRoleById(@PathVariable Long id) {
    return ok(roleService.getRoleById(id));
  }

  @Override
  @PostMapping
  @PreAuthorize("hasAuthority('roles:create')")
  public RootEntityResponse<DtoRole> createRole(@Valid @RequestBody DtoRoleIU dto) {
    return ok(roleService.createRole(dto));
  }

  @Override
  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('roles:update')")
  public RootEntityResponse<DtoRole> updateRole(@PathVariable Long id, @Valid @RequestBody DtoRoleIU dto) {
    return ok(roleService.updateRole(id, dto));
  }

  @Override
  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('roles:delete')")
  public RootEntityResponse<Boolean> deleteRole(@PathVariable Long id) {
    return ok(roleService.deleteRole(id));
  }

  @Override
  @PutMapping("/{roleId}/permissions")
  @PreAuthorize("hasAuthority('roles:update')")
  public RootEntityResponse<DtoRole> assignPermissionsToRole(
      @PathVariable Long roleId,
      @RequestBody Set<Long> permissionIds) {
    return ok(roleService.assignPermissionsToRole(roleId, permissionIds));
  }

  @Override
  @PutMapping("/users/{userId}/roles")
  @PreAuthorize("hasAuthority('users:manage')")
  public RootEntityResponse<Void> assignRolesToUser(
      @PathVariable Long userId,
      @Valid @RequestBody DtoUserRoleAssignment dto) {
    roleService.assignRolesToUser(userId, dto.getRoleIds());
    return ok(null);
  }

  // =============== Permission Endpoints ===============

  @Override
  @GetMapping("/permissions")
  @PreAuthorize("hasAuthority('roles:read')")
  public RootEntityResponse<List<DtoPermission>> getAllPermissions() {
    return ok(roleService.getAllPermissions());
  }

  @Override
  @GetMapping("/permissions/module/{module}")
  @PreAuthorize("hasAuthority('roles:read')")
  public RootEntityResponse<List<DtoPermission>> getPermissionsByModule(@PathVariable String module) {
    return ok(roleService.getPermissionsByModule(module));
  }
}
