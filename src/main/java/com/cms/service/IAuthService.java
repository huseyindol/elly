package com.cms.service;

import com.cms.dto.DtoAuthResponse;
import com.cms.dto.DtoGuestTokenRequest;
import com.cms.dto.DtoGuestTokenResponse;
import com.cms.dto.DtoLogin;
import com.cms.dto.DtoRefreshToken;
import com.cms.dto.DtoRegister;
import com.cms.dto.DtoTenantTokenResponse;

public interface IAuthService {
  DtoAuthResponse register(DtoRegister dtoRegister);

  DtoAuthResponse login(DtoLogin dtoLogin);

  DtoAuthResponse refreshToken(DtoRefreshToken dtoRefreshToken);

  DtoTenantTokenResponse getPublicToken(String tenantId);

  void verifyEmail(String token, String tenantId);

  DtoGuestTokenResponse getGuestToken(DtoGuestTokenRequest request);
}
