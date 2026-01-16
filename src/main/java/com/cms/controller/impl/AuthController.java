package com.cms.controller.impl;

import org.springframework.beans.factory.annotation.Value;
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
import com.cms.entity.User;
import com.cms.repository.UserRepository;
import com.cms.service.IAuthService;
import com.cms.util.CookieUtil;
import com.cms.util.UserUtil;

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

  @Value("${cookie.access-token.expiration:180}")
  private Integer accessTokenCookieExpiration; // Saniye cinsinden

  @Value("${cookie.refresh-token.expiration:360}")
  private Integer refreshTokenCookieExpiration; // Saniye cinsinden

  @Value("${cookie.user-code.expiration:360}")
  private Integer userCodeCookieExpiration; // Saniye cinsinden

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

    // Önce mevcut cookie'leri temizle (aynı isim, path ile Max-Age=0 gönder)
    // cookieUtil.clearCookie(httpResponse, "accessToken", "/");
    // cookieUtil.clearCookie(httpResponse, "refreshToken", "/");
    // cookieUtil.clearCookie(httpResponse, "userCode", "/");

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
