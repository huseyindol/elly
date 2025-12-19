package com.cms.controller;

import org.springframework.web.bind.annotation.RequestBody;

import com.cms.dto.DtoAuthResponse;
import com.cms.dto.DtoLogin;
import com.cms.dto.DtoRefreshToken;
import com.cms.dto.DtoRegister;
import com.cms.entity.RootEntityResponse;

import jakarta.validation.Valid;

public interface IAuthController {
  RootEntityResponse<DtoAuthResponse> register(@Valid @RequestBody DtoRegister dtoRegister);

  RootEntityResponse<DtoAuthResponse> login(@Valid @RequestBody DtoLogin dtoLogin);

  RootEntityResponse<DtoAuthResponse> refreshToken(@Valid @RequestBody DtoRefreshToken dtoRefreshToken);
}
