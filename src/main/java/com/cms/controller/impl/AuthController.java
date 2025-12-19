package com.cms.controller.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cms.controller.IAuthController;
import com.cms.dto.DtoAuthResponse;
import com.cms.dto.DtoLogin;
import com.cms.dto.DtoRefreshToken;
import com.cms.dto.DtoRegister;
import com.cms.entity.RootEntityResponse;
import com.cms.service.IAuthService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController extends BaseController implements IAuthController {

  @Autowired
  private IAuthService authService;

  @Override
  @PostMapping("/register")
  public RootEntityResponse<DtoAuthResponse> register(@Valid @RequestBody DtoRegister dtoRegister) {
    DtoAuthResponse response = authService.register(dtoRegister);
    return ok(response);
  }

  @Override
  @PostMapping("/login")
  public RootEntityResponse<DtoAuthResponse> login(@Valid @RequestBody DtoLogin dtoLogin) {
    DtoAuthResponse response = authService.login(dtoLogin);
    return ok(response);
  }

  @Override
  @PostMapping("/refresh")
  public RootEntityResponse<DtoAuthResponse> refreshToken(@Valid @RequestBody DtoRefreshToken dtoRefreshToken) {
    DtoAuthResponse response = authService.refreshToken(dtoRefreshToken);
    return ok(response);
  }
}
