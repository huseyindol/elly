package com.cms.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

/**
 * İstekteki access token'ı tek noktadan çözer.
 *
 * <p>Kaynak sırası:
 * <ol>
 *   <li><b>Authorization: Bearer &lt;jwt&gt;</b> — admin paneli, server-to-server,
 *       araç çağrıları (token açıkça gönderilir).</li>
 *   <li><b>{@code accessToken} httpOnly cookie</b> — tenant website (CSR). Tarayıcı
 *       {@code credentials: 'include'} ile cookie'yi otomatik gönderir; token JS'e
 *       hiç açılmaz. Sayfalar ISR/SSG kalabilir.</li>
 * </ol>
 *
 * <p>Bu sınıf yalnızca ham token string'ini döndürür; geçerlilik/imza kontrolü
 * çağıran filtreye aittir. Header strict (geçersizse 401), cookie lenient
 * (geçersizse anonim) yorumlanmalıdır — bkz. {@code JwtAuthenticationFilter}.
 */
public final class AuthTokenResolver {

  public static final String ACCESS_TOKEN_COOKIE = "accessToken";

  private AuthTokenResolver() {
    // utility
  }

  /** İstek Authorization Bearer header'ı taşıyor mu? (strict/lenient kararı için) */
  public static boolean hasBearerHeader(HttpServletRequest request) {
    String header = request.getHeader("Authorization");
    return header != null && header.startsWith("Bearer ");
  }

  /** Bearer header veya accessToken cookie'sinden ham JWT; ikisi de yoksa null. */
  public static String resolve(HttpServletRequest request) {
    String header = request.getHeader("Authorization");
    if (header != null && header.startsWith("Bearer ")) {
      String token = header.substring(7);
      return token.isBlank() ? null : token;
    }

    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if (ACCESS_TOKEN_COOKIE.equals(cookie.getName())) {
          String value = cookie.getValue();
          return (value != null && !value.isBlank()) ? value : null;
        }
      }
    }
    return null;
  }
}
