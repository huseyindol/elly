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
import com.cms.dto.DtoSocialLogin;
import com.cms.entity.User;

public interface IAuthService {
  DtoAuthResponse register(DtoRegister dtoRegister);

  DtoAuthResponse login(DtoLogin dtoLogin);

  /**
   * Sosyal giriş (Google/GitHub) — authorization code backend'de takas edilir; kullanıcı
   * bulunur/bağlanır/oluşturulur (yalnız TENANT rolü) ve normal login ile aynı yanıt döner.
   */
  DtoAuthResponse socialLogin(String provider, DtoSocialLogin dto);

  DtoAuthResponse refreshToken(DtoRefreshToken dtoRefreshToken);

  void verifyEmail(String token, String tenantId);

  /**
   * Verification email gönderir; URL caller tarafından geçilir
   * (tenant register → tenant frontend, admin create → panel frontend).
   */
  void sendVerificationEmail(User user, String tenantId, String verificationToken,
      String frontendUrl);

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
