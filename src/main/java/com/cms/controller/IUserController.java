package com.cms.controller;

import com.cms.dto.DtoChangePassword;
import com.cms.dto.DtoUserPermissions;
import com.cms.dto.DtoUserResponse;
import com.cms.dto.DtoUserUpdate;
import com.cms.entity.RootEntityResponse;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;

public interface IUserController {
  RootEntityResponse<DtoUserResponse> getMe();

  RootEntityResponse<DtoUserResponse> updateMe(@Valid @RequestBody DtoUserUpdate dto);

  RootEntityResponse<Void> changePassword(@Valid @RequestBody DtoChangePassword dto);

  RootEntityResponse<DtoUserPermissions> getMyPermissions();
}
