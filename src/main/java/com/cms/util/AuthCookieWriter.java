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

  // Cookie max-age'leri JWT sürelerinden türetilir (ms → sn). Tek kaynak = JWT_EXPIRATION /
  // JWT_REFRESH_EXPIRATION env'leri. Ayrı COOKIE_*_EXPIRATION env'leri YOK.
  @Value("${jwt.expiration:1800000}")
  private long jwtAccessExpirationMs;

  @Value("${jwt.refresh.expiration:604800000}")
  private long jwtRefreshExpirationMs;

  /**
   * Verilen auth yanıtından access/refresh/userCode/expiredDate cookie'lerini set eder.
   * Çağıran taraf tam token üretilmiş bir yanıt geçmelidir (MFA challenge sırasında çağrılmamalı).
   *
   * <p>userCode + expiredDate cookie'leri ACCESS süresince yaşar (panel/tenant frontend ile
   * tutarlı): access expire olduğunda silent refresh mekanizması zaten önceden devreye girer
   * ve cookie'ler yenilenir; refresh'in kendisi ayrıca refresh süresi kadar yaşar.
   */
  public void writeAuthCookies(HttpServletResponse response, DtoAuthResponse auth) {
    int accessMaxAge = (int) (jwtAccessExpirationMs / 1000);
    int refreshMaxAge = (int) (jwtRefreshExpirationMs / 1000);

    cookieUtil.setAccessTokenCookie(response, auth.getToken(), accessMaxAge);
    cookieUtil.setRefreshTokenCookie(response, auth.getRefreshToken(), refreshMaxAge);

    String userCode = auth.getUserCode() != null ? auth.getUserCode() : "";
    cookieUtil.setUserCodeCookie(response, userCode, accessMaxAge);

    cookieUtil.setCookie(response, "expiredDate", String.valueOf(auth.getExpiredDate()),
        accessMaxAge, false, true, "/");
  }
}
