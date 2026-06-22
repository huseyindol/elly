package com.cms.exception;

import org.springframework.http.HttpStatus;

/**
 * Tenant çözülemediğinde fırlatılır.
 *
 * <p>Authenticated bir istekte JWT'de {@code tenantId} claim'i yok/boş demektir —
 * "hangi DB'ye bakılacağı" belirsizdir. basedb fallback'i KALDIRILDIĞI için bu durum
 * sessizce yanlış/varsayılan DB'ye düşmek yerine net bu exception ile reddedilir
 * (hızlı teşhis: errorCode={@code TENANT_REQUIRED}).
 */
public class TenantRequiredException extends BaseException {

  public TenantRequiredException(String message) {
    super(message, HttpStatus.BAD_REQUEST, "TENANT_REQUIRED");
  }
}
