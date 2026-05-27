package com.cms.controller;

import com.cms.dto.DtoAuthResponse;
import com.cms.dto.DtoMfaDisableRequest;
import com.cms.dto.DtoMfaSetupResponse;
import com.cms.dto.DtoMfaSetupVerifyRequest;
import com.cms.dto.DtoMfaVerifyRequest;
import com.cms.entity.RootEntityResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * İki Faktörlü Kimlik Doğrulama (TOTP) endpoint'leri.
 * Tümü /api/v1/auth/mfa prefix'i ile erişilebilir.
 */
public interface IMfaController {

  /**
   * TOTP secret üret ve QR URI döner.
   * Kullanıcı Authenticator uygulamasına ekler.
   * Gerektirir: Geçerli JWT (setup tamamlanmamış olabilir).
   */
  @GetMapping("/setup")
  ResponseEntity<RootEntityResponse<DtoMfaSetupResponse>> setup();

  /**
   * Authenticator'dan alınan ilk kod ile setup'ı tamamla.
   * Başarılı → mfaEnabled=true, mfaSetupVerified=true.
   * Gerektirir: Geçerli JWT + 6 haneli TOTP kodu.
   */
  @PostMapping("/setup/verify")
  ResponseEntity<RootEntityResponse<String>> verifySetup(
      @Valid @RequestBody DtoMfaSetupVerifyRequest request);

  /**
   * Login akışının 2. adımı.
   * Gerektirir: mfaToken (login'den dönen) + 6 haneli TOTP kodu.
   * Başarılı → tam access + refresh JWT döner.
   */
  @PostMapping("/verify")
  ResponseEntity<RootEntityResponse<DtoAuthResponse>> verifyLogin(
      @Valid @RequestBody DtoMfaVerifyRequest request);

  /**
   * 2FA'yı deaktive et.
   * Gerektirir: Geçerli JWT + mevcut şifre.
   */
  @PostMapping("/disable")
  ResponseEntity<RootEntityResponse<String>> disable(
      @Valid @RequestBody DtoMfaDisableRequest request);
}
