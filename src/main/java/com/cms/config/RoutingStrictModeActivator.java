package com.cms.config;

import javax.sql.DataSource;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Uygulama tamamen ayağa kalktıktan sonra {@link TenantRoutingDataSource}'u strict mode'a alır.
 * Boot/seed sırasında null context için boot-fallback target kullanılabilsin diye en sonda çalışır
 * ({@link Ordered#LOWEST_PRECEDENCE}); ondan sonra her null context istek
 * {@link com.cms.exception.TenantRequiredException} ile reddedilir.
 */
@Component
@RequiredArgsConstructor
@Order(Ordered.LOWEST_PRECEDENCE)
public class RoutingStrictModeActivator {

  private final DataSource dataSource;

  @EventListener(ApplicationReadyEvent.class)
  public void activate() {
    if (dataSource instanceof TenantRoutingDataSource routing) {
      routing.enableStrict();
    }
  }
}
