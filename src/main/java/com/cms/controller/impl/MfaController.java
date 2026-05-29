package com.cms.controller.impl;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cms.config.JwtAuthenticationFilter;
import com.cms.config.TenantContext;
import com.cms.controller.IMfaController;
import com.cms.dto.DtoAuthResponse;
import com.cms.dto.DtoMfaDisableRequest;
import com.cms.dto.DtoMfaSetupResponse;
import com.cms.dto.DtoMfaSetupVerifyRequest;
import com.cms.dto.DtoMfaStatusResponse;
import com.cms.dto.DtoMfaVerifyRequest;
import com.cms.entity.RootEntityResponse;
import com.cms.exception.UnauthorizedException;
import com.cms.service.IAuthService;
import com.cms.util.AuthCookieWriter;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 2FA (TOTP) yönetim endpoint'leri.
 *
 * <p>Setup, verify (login adımı 2) ve disable işlemleri burada ele alınır.
 * /verify endpoint'i kimlik doğrulama gerektirmez (mfaToken yeterli).
 * Diğer endpoint'ler geçerli JWT gerektirir.
 */
@RestController
@RequestMapping("/api/v1/auth/mfa")
@RequiredArgsConstructor
public class MfaController implements IMfaController {

  private final IAuthService authService;
  private final AuthCookieWriter authCookieWriter;

  // ── GET /api/v1/auth/mfa/status ────────────────────────────────────────
  @Override
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<RootEntityResponse<DtoMfaStatusResponse>> status() {
    Long userId = getCurrentUserId();
    String tenantId = getCurrentTenantId();
    return ResponseEntity.ok(RootEntityResponse.ok(authService.getMfaStatus(userId, tenantId)));
  }

  // ── GET /api/v1/auth/mfa/setup ─────────────────────────────────────────
  @Override
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<RootEntityResponse<DtoMfaSetupResponse>> setup() {
    Long userId = getCurrentUserId();
    String tenantId = getCurrentTenantId();
    DtoMfaSetupResponse response = authService.initMfaSetup(userId, tenantId);
    return ResponseEntity.ok(RootEntityResponse.ok(response));
  }

  // ── POST /api/v1/auth/mfa/setup/verify ────────────────────────────────
  @Override
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<RootEntityResponse<String>> verifySetup(
      @Valid DtoMfaSetupVerifyRequest request) {
    Long userId = getCurrentUserId();
    String tenantId = getCurrentTenantId();
    authService.completeMfaSetup(userId, request, tenantId);
    return ResponseEntity.ok(RootEntityResponse.ok("2FA başarıyla etkinleştirildi."));
  }

  // ── POST /api/v1/auth/mfa/verify ──────────────────────────────────────
  // Bu endpoint login akışının 2. adımı: JWT gerektirmez, mfaToken yeterli.
  @Override
  public ResponseEntity<RootEntityResponse<DtoAuthResponse>> verifyLogin(
      @Valid DtoMfaVerifyRequest request, HttpServletResponse httpResponse) {
    DtoAuthResponse response = authService.verifyMfaLogin(request);
    // GAP-2: /login ile aynı HttpOnly oturum cookie'lerini set et (yollar arası drift kapatıldı)
    authCookieWriter.writeAuthCookies(httpResponse, response);
    return ResponseEntity.ok(RootEntityResponse.ok(response));
  }

  // ── POST /api/v1/auth/mfa/disable ─────────────────────────────────────
  @Override
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<RootEntityResponse<String>> disable(
      @Valid DtoMfaDisableRequest request) {
    Long userId = getCurrentUserId();
    String tenantId = getCurrentTenantId();
    authService.disableMfa(userId, request, tenantId);
    return ResponseEntity.ok(RootEntityResponse.ok("2FA başarıyla devre dışı bırakıldı."));
  }

  // ── Yardımcı Metodlar ──────────────────────────────────────────────────

  private Long getCurrentUserId() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || auth.getPrincipal() == null) {
      throw new UnauthorizedException("Kimlik doğrulaması gerekli.");
    }
    if (auth.getPrincipal() instanceof JwtAuthenticationFilter.CachedUserPrincipal principal) {
      return principal.getUserId();
    }
    throw new UnauthorizedException("Kullanıcı kimliği çözümlenemedi.");
  }

  /** JWT filter zaten TenantContext'i set eder; buradan okumak yeterli. */
  private String getCurrentTenantId() {
    return TenantContext.getTenantId();
  }
}
