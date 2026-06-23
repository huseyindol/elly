package com.cms.config;

import com.cms.exception.TenantRequiredException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * AbstractRoutingDataSource implementasyonu.
 *
 * <p>TenantContext'ten okunan tenant ID'ye göre doğru DataSource'a yönlendirir.
 * Her SQL sorgusundan önce {@link #determineCurrentLookupKey()} çağrılır.
 *
 * <p><b>Strict mode:</b> Uygulama hazır olduktan sonra ({@link RoutingStrictModeActivator}
 * ApplicationReadyEvent'te flip eder) context boşsa sessiz default datasource fallback'i
 * yerine {@link TenantRequiredException} fırlatır — "hangi DB?" belirsizliği sessiz hata
 * yerine net hata olur. Boot/seed sırasında strict=false kalır (Hibernate dialect tespiti,
 * tenant şema seed'i gibi başlangıç işleri için boot/fallback target gerekli).
 */
@Slf4j
public class TenantRoutingDataSource extends AbstractRoutingDataSource {

  /** Boot tamamlanınca true → null context için sessiz fallback yerine exception. */
  private volatile boolean strict = false;

  public void enableStrict() {
    this.strict = true;
    log.info("TenantRoutingDataSource strict mode ENABLED — null context => TenantRequiredException");
  }

  @Override
  protected Object determineCurrentLookupKey() {
    String tenantId = TenantContext.getTenantId();
    if (tenantId == null || tenantId.isBlank()) {
      if (strict) {
        throw new TenantRequiredException(
            "Tenant context bos — JWT tenantId claim veya /api/v1/public/{tid} URL gerekli");
      }
      log.debug("Routing datasource: context bos, boot/fallback target kullaniliyor");
      return null;
    }
    log.debug("Routing datasource to tenant: {}", tenantId);
    return tenantId;
  }
}
