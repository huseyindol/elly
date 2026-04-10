package com.cms.controller.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cms.config.TenantContext;
import com.cms.controller.IAuthController;
import com.cms.dto.DtoAuthResponse;
import com.cms.dto.DtoLogin;
import com.cms.dto.DtoRefreshToken;
import com.cms.dto.DtoRegister;
import com.cms.dto.DtoTenantTokenResponse;
import com.cms.entity.RootEntityResponse;
import com.cms.entity.User;
import com.cms.repository.UserRepository;
import com.cms.service.IAuthService;
import com.cms.util.CookieUtil;
import com.cms.util.JwtUtil;
import com.cms.util.UserUtil;

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
  private final UserRepository userRepository;
  private final UserUtil userUtil;
  private final CookieUtil cookieUtil;
  private final JwtUtil jwtUtil;

  @Value("${cookie.access-token.expiration:180}")
  private Integer accessTokenCookieExpiration; // Saniye cinsinden

  @Value("${cookie.refresh-token.expiration:360}")
  private Integer refreshTokenCookieExpiration; // Saniye cinsinden

  @Value("${cookie.user-code.expiration:360}")
  private Integer userCodeCookieExpiration; // Saniye cinsinden

  @GetMapping("/public-token/{tenantId}")
  public RootEntityResponse<DtoTenantTokenResponse> getPublicToken(@PathVariable String tenantId) {
    return ok(authService.getPublicToken(tenantId));
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

    // Cookie'lere token, refreshToken ve userCode ekle
    cookieUtil.setAccessTokenCookie(httpResponse, response.getToken(), accessTokenCookieExpiration);
    cookieUtil.setRefreshTokenCookie(httpResponse, response.getRefreshToken(), refreshTokenCookieExpiration);

    // UserCode: firstName ve lastName'in ilk harfleri
    User user = userRepository.findById(response.getUserId())
        .orElse(null);
    String userCode = userUtil.generateUserCode(user);
    cookieUtil.setUserCodeCookie(httpResponse, userCode, userCodeCookieExpiration);

    // Cookie'lere expiredDate ekle
    cookieUtil.setCookie(httpResponse, "expiredDate", String.valueOf(response.getExpiredDate()),
        refreshTokenCookieExpiration,
        false, true, "/");

    return ok(response);
  }

  @Override
  @PostMapping("/refresh")
  public RootEntityResponse<DtoAuthResponse> refreshToken(@Valid @RequestBody DtoRefreshToken dtoRefreshToken,
      HttpServletResponse httpResponse) {
    DtoAuthResponse response = authService.refreshToken(dtoRefreshToken);

    // Cookie'lere token, refreshToken ve userCode ekle
    cookieUtil.setAccessTokenCookie(httpResponse, response.getToken(), accessTokenCookieExpiration);
    cookieUtil.setRefreshTokenCookie(httpResponse, response.getRefreshToken(), refreshTokenCookieExpiration);

    // UserCode: firstName ve lastName'in ilk harfleri
    User user = userRepository.findById(response.getUserId())
        .orElse(null);
    String userCode = userUtil.generateUserCode(user);
    cookieUtil.setUserCodeCookie(httpResponse, userCode, userCodeCookieExpiration);

    // Cookie'lere expiredDate ekle
    cookieUtil.setCookie(httpResponse, "expiredDate", String.valueOf(response.getExpiredDate()),
        refreshTokenCookieExpiration,
        false, true, "/");

    return ok(response);
  }
}
