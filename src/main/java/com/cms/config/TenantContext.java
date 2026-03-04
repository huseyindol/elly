package com.cms.config;

import lombok.extern.slf4j.Slf4j;

/**
 * ThreadLocal tabanlı Tenant Context yönetimi.
 * Her HTTP isteği için aktif tenant bilgisini tutar.
 * 
 * IMPORTANT: Memory leak oluşmaması için request bittiğinde
 * mutlaka clear() çağrılmalıdır (JwtTenantFilter tarafından yapılır).
 */
@Slf4j
public final class TenantContext {

  private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

  private TenantContext() {
    // Utility class - instance oluşturulamaz
  }

  /**
   * Aktif tenant ID'sini set eder.
   *
   * @param tenantId tenant identifier
   */
  public static void setTenantId(String tenantId) {
    log.debug("Setting tenant context to: {}", tenantId);
    CURRENT_TENANT.set(tenantId);
  }

  /**
   * Aktif tenant ID'sini döndürür.
   *
   * @return current tenant ID veya null
   */
  public static String getTenantId() {
    return CURRENT_TENANT.get();
  }

  /**
   * ThreadLocal'i temizler.
   * Memory leak önleme için her request sonunda çağrılmalıdır.
   */
  public static void clear() {
    log.debug("Clearing tenant context");
    CURRENT_TENANT.remove();
  }
}
