package com.cms.controller;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.cms.dto.DtoAuthResponse;
import com.cms.dto.DtoGuestTokenRequest;
import com.cms.dto.DtoGuestTokenResponse;
import com.cms.dto.DtoLogin;
import com.cms.dto.DtoRefreshToken;
import com.cms.dto.DtoRegister;
import com.cms.entity.RootEntityResponse;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

public interface IAuthController {
  RootEntityResponse<DtoAuthResponse> register(@Valid @RequestBody DtoRegister dtoRegister);

  RootEntityResponse<DtoAuthResponse> login(@Valid @RequestBody DtoLogin dtoLogin,
      HttpServletResponse httpResponse);

  RootEntityResponse<DtoAuthResponse> refreshToken(@Valid @RequestBody DtoRefreshToken dtoRefreshToken,
      HttpServletResponse httpResponse);

  RootEntityResponse<Boolean> verifyEmail(@RequestParam String token, @RequestParam String tenantId);

  RootEntityResponse<DtoGuestTokenResponse> getGuestToken(@Valid @RequestBody DtoGuestTokenRequest request);
}
