package com.cms.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * AbstractRoutingDataSource implementasyonu.
 * TenantContext'ten okunan tenant ID'ye göre doğru DataSource'a yönlendirir.
 * 
 * Spring Data JPA/Hibernate bu DataSource'u kullandığında,
 * her SQL sorgusu çalıştırılmadan önce determineCurrentLookupKey() çağrılır
 * ve dönen key'e karşılık gelen DataSource seçilir.
 */
@Slf4j
public class TenantRoutingDataSource extends AbstractRoutingDataSource {

  /**
   * Aktif tenant'ın DataSource lookup key'ini döndürür.
   * TenantContext'te set edilmiş tenant ID'yi kullanır.
   *
   * @return tenant ID (DataSource map'indeki key)
   */
  @Override
  protected Object determineCurrentLookupKey() {
    String tenantId = TenantContext.getTenantId();
    log.debug("Routing datasource to tenant: {}", tenantId);
    return tenantId;
  }
}
