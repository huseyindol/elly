package com.cms.config;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.zaxxer.hikari.HikariDataSource;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Multi-Tenant DataSource konfigürasyonu.
 * 
 * application.properties'den tenant datasource bilgilerini okur
 * ve her tenant için ayrı HikariCP connection pool oluşturur.
 * 
 * Cloud Run / K8s gibi yatayda ölçeklenen ortamlar için
 * HikariCP ayarları optimize edilmiştir.
 */
@Configuration
@Slf4j
public class DataSourceConfig {

  /**
   * Tenant datasource properties'lerini yükler.
   * application.properties'deki app.tenants.* prefix'li değerleri okur.
   */
  @Bean
  @ConfigurationProperties(prefix = "app.tenants")
  public TenantDataSourceProperties tenantDataSourceProperties() {
    return new TenantDataSourceProperties();
  }

  /**
   * TenantRoutingDataSource bean'i oluşturur.
   * Tüm tenant DataSource'larını map olarak set eder.
   * Default tenant'ı belirler.
   */
  @Bean
  public DataSource dataSource(TenantDataSourceProperties properties) {
    TenantRoutingDataSource routingDataSource = new TenantRoutingDataSource();

    Map<Object, Object> targetDataSources = new HashMap<>();

    properties.getDatasources().forEach((tenantId, config) -> {
      log.info("Configuring datasource for tenant: {}", tenantId);
      DataSource tenantDataSource = createHikariDataSource(tenantId, config);
      targetDataSources.put(tenantId, tenantDataSource);
    });

    routingDataSource.setTargetDataSources(targetDataSources);

    // Default tenant datasource
    String defaultTenant = properties.getDefaultTenant();
    if (defaultTenant != null && targetDataSources.containsKey(defaultTenant)) {
      routingDataSource.setDefaultTargetDataSource(targetDataSources.get(defaultTenant));
      log.info("Default tenant datasource set to: {}", defaultTenant);
    }

    routingDataSource.afterPropertiesSet();

    log.info("Multi-tenant routing datasource configured with {} tenants", targetDataSources.size());
    return routingDataSource;
  }

  /**
   * Tek bir tenant için optimize edilmiş HikariCP DataSource oluşturur.
   * 
   * Pool ayarları Cloud Run / K8s auto-scaling ortamları için tune edilmiştir:
   * - maximumPoolSize: 10 (her instance başına)
   * - minimumIdle: 2 (cold-start'ta hızlı response)
   * - connectionTimeout: 30s
   * - idleTimeout: 10 dakika
   * - maxLifetime: 30 dakika (DB tarafındaki timeout'lardan kısa tutulur)
   */
  private DataSource createHikariDataSource(String tenantId, TenantDataSourceConfig config) {
    HikariDataSource dataSource = new HikariDataSource();
    dataSource.setPoolName("HikariPool-" + tenantId);
    dataSource.setJdbcUrl(config.getUrl());
    dataSource.setUsername(config.getUsername());
    dataSource.setPassword(config.getPassword());
    dataSource.setDriverClassName("org.postgresql.Driver");

    // Schema ayarı
    if (config.getSchema() != null && !config.getSchema().isBlank()) {
      dataSource.setSchema(config.getSchema());
      dataSource.addDataSourceProperty("currentSchema", config.getSchema());
    }

    // ==========================================
    // HikariCP Best-Practice Pool Ayarları
    // (Yatay ölçekleme ortamları için optimize)
    // ==========================================
    dataSource.setMaximumPoolSize(config.getMaxPoolSize() != null ? config.getMaxPoolSize() : 10);
    dataSource.setMinimumIdle(config.getMinIdle() != null ? config.getMinIdle() : 2);
    dataSource.setConnectionTimeout(30_000); // 30 saniye
    dataSource.setIdleTimeout(600_000); // 10 dakika
    dataSource.setMaxLifetime(1_800_000); // 30 dakika
    dataSource.setLeakDetectionThreshold(60_000); // 60 saniye - connection leak tespiti

    log.info("Created HikariCP pool '{}' -> url={}, schema={}, maxPool={}, minIdle={}",
        dataSource.getPoolName(),
        config.getUrl(),
        config.getSchema(),
        dataSource.getMaximumPoolSize(),
        dataSource.getMinimumIdle());

    return dataSource;
  }

  // ==========================================
  // Inner Configuration Classes
  // ==========================================

  /**
   * Tüm tenant datasource properties'lerini tutar.
   * Binding: app.tenants.*
   */
  @Getter
  @Setter
  public static class TenantDataSourceProperties {
    /**
     * Default tenant ID (token olmadan gelen istekler için)
     * Binding: app.tenants.default-tenant
     */
    private String defaultTenant;

    /**
     * Tenant -> DataSource config map
     * Binding: app.tenants.datasources.{tenantId}.*
     */
    private Map<String, TenantDataSourceConfig> datasources = new HashMap<>();
  }

  /**
   * Tek bir tenant'ın datasource konfigürasyonu.
   * Binding: app.tenants.datasources.{tenantId}.*
   */
  @Getter
  @Setter
  public static class TenantDataSourceConfig {
    private String url;
    private String username;
    private String password;
    private String schema;
    private Integer maxPoolSize;
    private Integer minIdle;
    /** Tenant'a özgü "from" adresi. Null ise global mail.from kullanılır. */
    private String mailFrom;
  }
}
