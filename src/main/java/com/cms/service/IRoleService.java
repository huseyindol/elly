package com.cms.service;

import java.util.List;
import java.util.Set;

import com.cms.dto.DtoPermission;
import com.cms.dto.DtoRole;
import com.cms.dto.DtoRoleIU;

public interface IRoleService {

  List<DtoRole> getAllRoles();

  DtoRole getRoleById(Long id);

  DtoRole createRole(DtoRoleIU dto);

  DtoRole updateRole(Long id, DtoRoleIU dto);

  Boolean deleteRole(Long id);

  DtoRole assignPermissionsToRole(Long roleId, Set<Long> permissionIds);

  void assignRolesToUser(Long userId, Set<Long> roleIds);

  List<DtoPermission> getAllPermissions();

  List<DtoPermission> getPermissionsByModule(String module);
}
