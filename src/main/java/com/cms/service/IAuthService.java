package com.cms.service;

import com.cms.dto.DtoAuthResponse;
import com.cms.dto.DtoGuestTokenRequest;
import com.cms.dto.DtoGuestTokenResponse;
import com.cms.dto.DtoLogin;
import com.cms.dto.DtoMfaDisableRequest;
import com.cms.dto.DtoMfaSetupResponse;
import com.cms.dto.DtoMfaSetupVerifyRequest;
import com.cms.dto.DtoMfaStatusResponse;
import com.cms.dto.DtoMfaVerifyRequest;
import com.cms.dto.DtoRefreshToken;
import com.cms.dto.DtoRegister;
import com.cms.dto.DtoTenantTokenResponse;

public interface IAuthService {
  DtoAuthResponse register(DtoRegister dtoRegister);

  DtoAuthResponse login(DtoLogin dtoLogin);

  DtoAuthResponse refreshToken(DtoRefreshToken dtoRefreshToken);

  DtoTenantTokenResponse getPublicToken(String tenantId);

  void verifyEmail(String token, String tenantId);

  // ── Guest Chat (anonim website ziyaretçisi) ──────────────────────────────
  /**
   * Anonim ziyaretçi için kısa ömürlü guest chat token'ı üretir.
   * tenantId zorunlu ve geçerli bir tenant olmalı; aksi halde 400.
   */
  DtoGuestTokenResponse getGuestToken(DtoGuestTokenRequest request);

  // ── 2FA (TOTP) ──────────────────────────────────────────────────────────

  /** Secret + QR URI üret; DB'ye kaydet (mfaSetupVerified=false) */
  DtoMfaSetupResponse initMfaSetup(Long userId, String tenantId);

  /** İlk kodu doğrula; mfaEnabled=true, mfaSetupVerified=true yap */
  void completeMfaSetup(Long userId, DtoMfaSetupVerifyRequest request, String tenantId);

  /** Login akışı 2. adım: MFA token + TOTP kodu → tam JWT döner */
  DtoAuthResponse verifyMfaLogin(DtoMfaVerifyRequest request);

  /** 2FA'yı deaktive et; şifre doğrulaması gerekir */
  void disableMfa(Long userId, DtoMfaDisableRequest request, String tenantId);

  /** Mevcut kullanıcının 2FA durumunu döner (panel "açık/kapalı" göstergesi için) */
  DtoMfaStatusResponse getMfaStatus(Long userId, String tenantId);
}
