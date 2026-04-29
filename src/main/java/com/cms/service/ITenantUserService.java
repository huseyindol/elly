package com.cms.service;

import java.util.List;

import com.cms.dto.DtoAdminUserCreate;
import com.cms.dto.DtoAdminUserUpdate;
import com.cms.dto.DtoUserResponse;

public interface ITenantUserService {

  DtoUserResponse createUser(String tenantId, DtoAdminUserCreate dto);

  List<DtoUserResponse> getAllUsers(String tenantId);

  DtoUserResponse getUserById(String tenantId, Long id);

  DtoUserResponse updateUser(String tenantId, Long id, DtoAdminUserUpdate dto);

  void deleteUser(String tenantId, Long id);

  DtoUserResponse setUserStatus(String tenantId, Long id, boolean isActive);
}
