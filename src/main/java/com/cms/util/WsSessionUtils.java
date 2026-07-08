package com.cms.util;

import java.util.Map;

import org.springframework.messaging.simp.SimpMessageHeaderAccessor;

import com.cms.exception.UnauthorizedException;

/**
 * STOMP session attribute'larından kullanıcı bilgisi okuma yardımcıları.
 * ChatWebSocketSecurityConfig CONNECT'te userId/username/tenantId/loginSource attribute'larını
 * set eder. ChatWebSocketController'daki private helper'ların paylaşılabilir hali — yeni
 * WS controller'ları (ör. CallSignalController) kopya yerine bunu kullanır.
 */
public final class WsSessionUtils {

  private WsSessionUtils() {
  }

  private static Map<String, Object> attrs(SimpMessageHeaderAccessor accessor) {
    Map<String, Object> attrs = accessor.getSessionAttributes();
    if (attrs == null) {
      throw new UnauthorizedException("No session attributes");
    }
    return attrs;
  }

  /** userId (zorunlu) — yoksa UnauthorizedException. */
  public static Long requireUserId(SimpMessageHeaderAccessor accessor) {
    Object userId = attrs(accessor).get("userId");
    if (userId == null) {
      throw new UnauthorizedException("userId not found in session");
    }
    return (userId instanceof Long l) ? l : Long.valueOf(userId.toString());
  }

  /** username (yoksa "unknown"). */
  public static String username(SimpMessageHeaderAccessor accessor) {
    Object username = attrs(accessor).get("username");
    return username != null ? username.toString() : "unknown";
  }

  /** tenantId (opsiyonel — null dönebilir). */
  public static String optionalTenant(SimpMessageHeaderAccessor accessor) {
    Object tenantId = attrs(accessor).get("tenantId");
    if (tenantId == null) {
      return null;
    }
    String value = tenantId.toString().trim();
    return value.isEmpty() ? null : value;
  }

  /** loginSource (yoksa null). "website"=anonim guest; "TENANT"/"tenant"=tenant-site kullanıcısı. */
  public static String loginSource(SimpMessageHeaderAccessor accessor) {
    Object loginSource = attrs(accessor).get("loginSource");
    return loginSource != null ? loginSource.toString() : null;
  }
}
