package com.cms.util;

import com.cms.dto.DtoAuthResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Başarılı authentication sonrası oturum cookie'lerini (access / refresh / userCode /
 * expiredDate) tek noktadan yazar.
 *
 * <p>{@code /login}, {@code /refresh} ve {@code /mfa/verify} aynı HttpOnly cookie setini
 * üretsin diye paylaşılır. Daha önce bu mantık her controller'da elle kopyalanıyordu;
 * {@code /mfa/verify} ise hiç cookie set etmiyordu (yollar arası drift). Tek kaynak bu
 * drift'i kapatır.</p>
 *
 * <p>{@code userCode} doğrudan {@link DtoAuthResponse}'tan okunur — servis katmanı bunu
 * kullanıcı doğru tenant context'inde (yüklü entity üzerinden) üretip yanıta koyar. Böylece
 * burada DB sorgusu / TenantContext bağımlılığı yoktur; MFA akışında userCode'un boş kalma
 * sorunu da bu sayede oluşmaz.</p>
 */
@Component
@RequiredArgsConstructor
public class AuthCookieWriter {

  private final CookieUtil cookieUtil;

  @Value("${cookie.access-token.expiration:180}")
  private Integer accessTokenCookieExpiration; // saniye

  @Value("${cookie.refresh-token.expiration:360}")
  private Integer refreshTokenCookieExpiration; // saniye

  @Value("${cookie.user-code.expiration:360}")
  private Integer userCodeCookieExpiration; // saniye

  /**
   * Verilen auth yanıtından access/refresh/userCode/expiredDate cookie'lerini set eder.
   * Çağıran taraf tam token üretilmiş bir yanıt geçmelidir (MFA challenge sırasında çağrılmamalı).
   */
  public void writeAuthCookies(HttpServletResponse response, DtoAuthResponse auth) {
    cookieUtil.setAccessTokenCookie(response, auth.getToken(), accessTokenCookieExpiration);
    cookieUtil.setRefreshTokenCookie(response, auth.getRefreshToken(), refreshTokenCookieExpiration);

    // userCode servis tarafından (kullanıcı doğru context'te yüklüyken) zaten üretildi
    String userCode = auth.getUserCode() != null ? auth.getUserCode() : "";
    cookieUtil.setUserCodeCookie(response, userCode, userCodeCookieExpiration);

    cookieUtil.setCookie(response, "expiredDate", String.valueOf(auth.getExpiredDate()),
        refreshTokenCookieExpiration, false, true, "/");
  }
}
