package com.cms.controller.impl;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.config.TenantContext;
import com.cms.controller.IAuthController;
import com.cms.dto.DtoAuthResponse;
import com.cms.dto.DtoLogin;
import com.cms.dto.DtoRefreshToken;
import com.cms.dto.DtoRegister;
import com.cms.dto.DtoGuestTokenRequest;
import com.cms.dto.DtoGuestTokenResponse;
import com.cms.dto.DtoTenantTokenResponse;
import com.cms.entity.RootEntityResponse;
import com.cms.service.IAuthService;
import com.cms.util.AuthCookieWriter;
import com.cms.util.JwtUtil;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController extends BaseController implements IAuthController {

  private final IAuthService authService;
  private final JwtUtil jwtUtil;
  private final AuthCookieWriter authCookieWriter;

  @GetMapping("/public-token/{tenantId}")
  public RootEntityResponse<DtoTenantTokenResponse> getPublicToken(@PathVariable String tenantId) {
    return ok(authService.getPublicToken(tenantId));
  }

  /**
   * Anonim website ziyaretçisi için guest chat token'ı.
   * Kayıt/giriş gerektirmez — sadece displayName + tenantId. /api/v1/auth/** zaten permitAll.
   */
  @PostMapping("/guest-token")
  public RootEntityResponse<DtoGuestTokenResponse> getGuestToken(
      @Valid @RequestBody DtoGuestTokenRequest request) {
    return ok(authService.getGuestToken(request));
  }

  @GetMapping("/decode")
  public RootEntityResponse<Map<String, Object>> decodeToken(
      @RequestHeader("Authorization") String authHeader) {
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      return error("Missing or invalid Authorization header");
    }
    String jwt = authHeader.substring(7);
    Map<String, Object> claims = new LinkedHashMap<>();
    claims.put("username", jwtUtil.extractUsername(jwt));
    claims.put("loginSource", jwtUtil.extractLoginSource(jwt));
    claims.put("userId", jwtUtil.extractUserId(jwt));
    claims.put("tenantId", jwtUtil.extractTenantId(jwt));
    claims.put("tokenVersion", jwtUtil.extractTokenVersion(jwt));
    claims.put("expiration", jwtUtil.extractExpiration(jwt));
    claims.put("currentTenantContext", TenantContext.getTenantId());
    return ok(claims);
  }

  @Override
  @PostMapping("/register")
  public RootEntityResponse<DtoAuthResponse> register(@Valid @RequestBody DtoRegister dtoRegister) {
    DtoAuthResponse response = authService.register(dtoRegister);
    return ok(response);
  }

  @Override
  @PostMapping("/login")
  public RootEntityResponse<DtoAuthResponse> login(@Valid @RequestBody DtoLogin dtoLogin,
      HttpServletResponse httpResponse) {
    DtoAuthResponse response = authService.login(dtoLogin);

    // 2FA gerekiyorsa tam token henüz yok (sadece mfaToken) — oturum cookie'leri YAZMA.
    // Kullanıcı /api/v1/auth/mfa/verify'i tamamlayınca cookie'ler orada set edilir.
    if (!Boolean.TRUE.equals(response.getMfaRequired())) {
      authCookieWriter.writeAuthCookies(httpResponse, response);
    }

    return ok(response);
  }

  @Override
  @GetMapping("/verify")
  public RootEntityResponse<Boolean> verifyEmail(@RequestParam String token, @RequestParam String tenantId) {
    authService.verifyEmail(token, tenantId);
    return ok(true);
  }

  @Override
  @PostMapping("/refresh")
  public RootEntityResponse<DtoAuthResponse> refreshToken(@Valid @RequestBody DtoRefreshToken dtoRefreshToken,
      HttpServletResponse httpResponse) {
    DtoAuthResponse response = authService.refreshToken(dtoRefreshToken);
    authCookieWriter.writeAuthCookies(httpResponse, response);
    return ok(response);
  }
}
