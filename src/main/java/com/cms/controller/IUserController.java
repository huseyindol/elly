package com.cms.controller;

import java.util.List;

import com.cms.dto.DtoAdminUserUpdate;
import com.cms.dto.DtoChangePassword;
import com.cms.dto.DtoCreateUser;
import com.cms.dto.DtoUserPermissions;
import com.cms.dto.DtoUserResponse;
import com.cms.dto.DtoUserUpdate;
import com.cms.entity.RootEntityResponse;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

public interface IUserController {
  RootEntityResponse<DtoUserResponse> getMe();

  RootEntityResponse<DtoUserResponse> updateMe(@Valid @RequestBody DtoUserUpdate dto);

  RootEntityResponse<Void> changePassword(@Valid @RequestBody DtoChangePassword dto);

  RootEntityResponse<DtoUserPermissions> getMyPermissions();

  RootEntityResponse<List<DtoUserResponse>> getAllUsers(@RequestParam(required = false) String audience);

  RootEntityResponse<DtoUserResponse> getUserById(@PathVariable Long id);

  RootEntityResponse<DtoUserResponse> createUser(@Valid @RequestBody DtoCreateUser request);

  RootEntityResponse<DtoUserResponse> updateUser(@PathVariable Long id,
      @Valid @RequestBody DtoAdminUserUpdate request);

  RootEntityResponse<Void> deleteUser(@PathVariable Long id);

  RootEntityResponse<DtoUserResponse> setUserStatus(@PathVariable Long id, @RequestParam boolean isActive);
}

