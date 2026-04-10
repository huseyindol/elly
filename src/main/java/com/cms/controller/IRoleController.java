package com.cms.controller;

import java.util.List;
import java.util.Set;

import com.cms.dto.DtoPermission;
import com.cms.dto.DtoRole;
import com.cms.dto.DtoRoleIU;
import com.cms.dto.DtoUserRoleAssignment;
import com.cms.entity.RootEntityResponse;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;

public interface IRoleController {
  RootEntityResponse<List<DtoRole>> getAllRoles();

  RootEntityResponse<DtoRole> getRoleById(Long id);

  RootEntityResponse<DtoRole> createRole(@Valid @RequestBody DtoRoleIU dto);

  RootEntityResponse<DtoRole> updateRole(Long id, @Valid @RequestBody DtoRoleIU dto);

  RootEntityResponse<Boolean> deleteRole(Long id);

  RootEntityResponse<DtoRole> assignPermissionsToRole(Long roleId, Set<Long> permissionIds);

  RootEntityResponse<Void> assignRolesToUser(Long userId, @Valid @RequestBody DtoUserRoleAssignment dto);

  RootEntityResponse<List<DtoPermission>> getAllPermissions();

  RootEntityResponse<List<DtoPermission>> getPermissionsByModule(String module);
}
