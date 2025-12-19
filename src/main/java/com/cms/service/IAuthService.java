package com.cms.service;

import com.cms.dto.DtoAuthResponse;
import com.cms.dto.DtoLogin;
import com.cms.dto.DtoRefreshToken;
import com.cms.dto.DtoRegister;

public interface IAuthService {
  DtoAuthResponse register(DtoRegister dtoRegister);

  DtoAuthResponse login(DtoLogin dtoLogin);

  DtoAuthResponse refreshToken(DtoRefreshToken dtoRefreshToken);
}
