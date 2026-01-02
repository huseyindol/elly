package com.cms.config;

import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Application genel konfigürasyonları
 * Timezone, locale gibi genel ayarlar burada tanımlanır
 */
@Configuration
public class AppConfig {

  @Value("${app.timezone:Europe/Istanbul}")
  private String timezone;

  /**
   * Application timezone'unu döndürür
   * Default: Europe/Istanbul (GMT+3)
   * 
   * @return ZoneId instance
   */
  @Bean
  public ZoneId applicationZoneId() {
    return ZoneId.of(timezone);
  }

  /**
   * Application timezone string'ini döndürür
   * 
   * @return Timezone string
   */
  public String getTimezone() {
    return timezone;
  }
}
