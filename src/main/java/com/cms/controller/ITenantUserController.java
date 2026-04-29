package com.cms.controller;

import java.util.List;

import com.cms.dto.DtoAdminUserCreate;
import com.cms.dto.DtoAdminUserUpdate;
import com.cms.dto.DtoUserResponse;
import com.cms.entity.RootEntityResponse;

import jakarta.validation.Valid;

public interface ITenantUserController {

  RootEntityResponse<DtoUserResponse> createUser(String tenantId, @Valid DtoAdminUserCreate dto);

  RootEntityResponse<List<DtoUserResponse>> getAllUsers(String tenantId);

  RootEntityResponse<DtoUserResponse> getUserById(String tenantId, Long id);

  RootEntityResponse<DtoUserResponse> updateUser(String tenantId, Long id, @Valid DtoAdminUserUpdate dto);

  RootEntityResponse<Void> deleteUser(String tenantId, Long id);

  RootEntityResponse<DtoUserResponse> setUserStatus(String tenantId, Long id, boolean isActive);
}
