package com.cms.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Cookie işlemleri için utility metodları
 */
@Component
public class CookieUtil {

  @Autowired
  private ZoneId applicationZoneId;

  /**
   * Mevcut cookie'yi temizler (Max-Age=0 ile)
   * Aynı isim, path ve domain ile cookie gönderilirse tarayıcı eski cookie'yi
   * override eder
   * 
   * @param response   HttpServletResponse
   * @param cookieName Cookie adı
   * @param path       Cookie path'i
   */
  public void clearCookie(HttpServletResponse response, String cookieName, String path) {
    Cookie cookie = new Cookie(cookieName, "");
    cookie.setPath(path);
    cookie.setMaxAge(0); // Cookie'yi hemen sil
    response.addCookie(cookie);
  }

  @Autowired
  private org.springframework.core.env.Environment environment;

  /**
   * Cookie oluşturur ve response'a ekler
   * 
   * @param response      HttpServletResponse
   * @param cookieName    Cookie adı
   * @param cookieValue   Cookie değeri
   * @param maxAgeSeconds Max-Age (saniye cinsinden)
   * @param httpOnly      HttpOnly flag
   * @param secure        Secure flag
   * @param path          Cookie path'i
   */
  public void setCookie(HttpServletResponse response, String cookieName, String cookieValue,
      int maxAgeSeconds, boolean httpOnly, boolean secure, String path) {
    ZonedDateTime expiration = ZonedDateTime.now(applicationZoneId).plusSeconds(maxAgeSeconds);

    // Aktif profili kontrol et (null check ekle)
    String[] activeProfiles = environment.getActiveProfiles();
    boolean isLocal = activeProfiles.length == 0 || java.util.Arrays.asList(activeProfiles).contains("local")
        || java.util.Arrays.asList(activeProfiles).contains("dev");

    // Local ortamda SameSite=None kullan (cross-origin çalışması için)
    // Production ortamda domain belirt (.huseyindol.site) ve SameSite=Lax/Strict
    // kullan

    StringBuilder cookieBuilder = new StringBuilder();
    cookieBuilder.append(String.format("%s=%s; Path=%s; Max-Age=%d; Expires=%s",
        cookieName, cookieValue, path, maxAgeSeconds,
        expiration.format(DateTimeFormatter.RFC_1123_DATE_TIME)));

    if (!isLocal) {
      cookieBuilder.append("; Domain=.huseyindol.site");
    }

    if (httpOnly) {
      cookieBuilder.append("; HttpOnly");
    }
    if (secure) {
      cookieBuilder.append("; Secure");
    }

    // Localde frontend (localhost:3000) backend'e (remote) bağlanıyorsa
    // SameSite=None şart
    // Ancak backend de localdeyse Strict olabilir.
    // Kullanıcının senaryosu: Local Frontend -> Remote Backend gibi duruyor.
    if (isLocal) {
      cookieBuilder.append("; SameSite=Strict");
    } else {
      cookieBuilder.append("; SameSite=Lax"); // Subdomain paylaşımı için Lax daha güvenli ve uyumlu
    }

    response.addHeader("Set-Cookie", cookieBuilder.toString());
  }

  /**
   * Access token cookie'si oluşturur
   */
  public void setAccessTokenCookie(HttpServletResponse response, String token, int maxAgeSeconds) {
    setCookie(response, "accessToken", token, maxAgeSeconds, true, true, "/");
  }

  /**
   * Refresh token cookie'si oluşturur
   */
  public void setRefreshTokenCookie(HttpServletResponse response, String refreshToken, int maxAgeSeconds) {
    setCookie(response, "refreshToken", refreshToken, maxAgeSeconds, true, true, "/");
  }

  /**
   * User code cookie'si oluşturur
   */
  public void setUserCodeCookie(HttpServletResponse response, String userCode, int maxAgeSeconds) {
    setCookie(response, "userCode", userCode, maxAgeSeconds, false, true, "/");
  }
}
