package com.cms.service;

import com.cms.dto.DtoChangePassword;
import com.cms.dto.DtoUserPermissions;
import com.cms.dto.DtoUserResponse;
import com.cms.dto.DtoUserUpdate;

public interface IUserService {
  DtoUserResponse getMe(String username);

  DtoUserResponse updateMe(String username, DtoUserUpdate dto);

  void changePassword(String username, DtoChangePassword dto);

  DtoUserPermissions getMyPermissions(String username);
}
