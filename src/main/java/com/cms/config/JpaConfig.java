package com.cms.config;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewFilter;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * JPA/Hibernate konfigürasyonu Multi-Tenant DataSource ile.
 *
 * hbm2ddl.auto stratejisi {@code app.jpa.ddl-auto} property'sinden okunur
 * (env: {@code JPA_DDL_AUTO}):
 *   - update   : entity farklarini otomatik ALTER TABLE yapar (LOKAL default)
 *   - validate : sadece dogrular, degistirmez (PROD ONERILEN)
 *   - none     : Hibernate hic dokunmaz
 *
 * Startup sirasinda default tenant uzerinde otomatik calisir. Diger
 * tenant'lar icin {@link #initializeTenantSchemas()} metodu
 * ApplicationReadyEvent'te her tenant datasource'unda ayni stratejiyi tetikler.
 */
@Configuration
@EnableJpaRepositories(basePackages = {"com.cms.repository"})
@EnableJpaAuditing
@EnableTransactionManagement
@RequiredArgsConstructor
@Slf4j
public class JpaConfig {

  private final DataSourceConfig.TenantDataSourceProperties tenantProperties;

  @Value("${app.jpa.ddl-auto:update}")
  private String ddlAuto;

  /**
   * LocalContainerEntityManagerFactoryBean - Hibernate SessionFactory yerine JPA
   * standard.
   * TenantRoutingDataSource'u kullanir.
   * hbm2ddl.auto stratejisi {@code app.jpa.ddl-auto} property'sinden gelir.
   */
  @Bean
  public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
    ensureAiUsageLogsTable();
    LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
    em.setDataSource(dataSource);
    em.setPackagesToScan("com.cms.entity");

    HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
    // generateDdl sadece ddl-auto != none/validate icin anlamli
    vendorAdapter.setGenerateDdl(!"none".equalsIgnoreCase(ddlAuto) && !"validate".equalsIgnoreCase(ddlAuto));
    vendorAdapter.setShowSql(true);
    em.setJpaVendorAdapter(vendorAdapter);

    log.info("JPA hbm2ddl.auto strategy: {}", ddlAuto);

    Map<String, Object> properties = new HashMap<>();
    properties.put("hibernate.hbm2ddl.auto", ddlAuto);
    // properties.put("hibernate.dialect",
    // "org.hibernate.dialect.PostgreSQLDialect");
    properties.put("hibernate.format_sql", "true");
    // Spring Boot'un default naming strategy - camelCase -> snake_case
    properties.put("hibernate.physical_naming_strategy",
        "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy");

    // Default tenant'ın schema'sını set et
    String defaultTenant = tenantProperties.getDefaultTenant();
    if (defaultTenant != null && tenantProperties.getDatasources().containsKey(defaultTenant)) {
      String schema = tenantProperties.getDatasources().get(defaultTenant).getSchema();
      if (schema != null && !schema.isBlank()) {
        properties.put("hibernate.default_schema", schema);
      }
    }

    em.setJpaPropertyMap(properties);

    return em;
  }

  /**
   * JPA Transaction Manager - EntityManagerFactory ile bağlantılı.
   */
  @Bean
  public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
    JpaTransactionManager transactionManager = new JpaTransactionManager();
    transactionManager.setEntityManagerFactory(entityManagerFactory);
    return transactionManager;
  }

  /**
   * Open EntityManager In View (OSIV) filtresi.
   * Hibernate session'ını HTTP request boyunca açık tutar.
   * Spring Boot auto-config devre dışı olduğu için manuel tanımlanır.
   * Bu sayede lazy-loaded koleksiyonlar (örn: Component.forms) JSON
   * serialization sırasında sorunsuz yüklenir.
   */
  @Bean
  public OpenEntityManagerInViewFilter openEntityManagerInViewFilter() {
    return new OpenEntityManagerInViewFilter();
  }

  /**
   * Uygulama hazır olduğunda, default tenant dışındaki tüm tenant'lar için
   * Hibernate schema update'i tetikler.
   * 
   * Her tenant'ın datasource'unu geçici EntityManagerFactory ile başlatarak
   * tabloların oluşturulmasını sağlar.
   */
  @EventListener(ApplicationReadyEvent.class)
  public void initializeTenantSchemas() {
    if ("validate".equalsIgnoreCase(ddlAuto) || "none".equalsIgnoreCase(ddlAuto)) {
      log.info("Skipping tenant schema init — ddl-auto={}", ddlAuto);
      return;
    }

    String defaultTenant = tenantProperties.getDefaultTenant();

    tenantProperties.getDatasources().forEach((tenantId, config) -> {
      if (tenantId.equals(defaultTenant)) {
        log.info("Skipping schema init for default tenant '{}' (already initialized)", tenantId);
        return;
      }

      log.info("Initializing schema for tenant: {}", tenantId);
      try {
        TenantContext.setTenantId(tenantId);

        // Bu tenant'ın datasource'unda EntityManagerFactory oluşturarak
        // Hibernate'in hbm2ddl.auto=update çalıştırmasını tetikle
        LocalContainerEntityManagerFactoryBean tempEmf = new LocalContainerEntityManagerFactoryBean();

        // Bu tenant için ayrı HikariDataSource oluştur (geçici)
        com.zaxxer.hikari.HikariDataSource ds = new com.zaxxer.hikari.HikariDataSource();
        ds.setJdbcUrl(config.getUrl());
        ds.setUsername(config.getUsername());
        ds.setPassword(config.getPassword());
        ds.setDriverClassName("org.postgresql.Driver");
        ds.setMaximumPoolSize(2);
        ds.setMinimumIdle(1);
        if (config.getSchema() != null && !config.getSchema().isBlank()) {
          ds.setSchema(config.getSchema());
          ds.addDataSourceProperty("currentSchema", config.getSchema());
        }

        tempEmf.setDataSource(ds);
        tempEmf.setPackagesToScan("com.cms.entity");

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        vendorAdapter.setGenerateDdl(!"none".equalsIgnoreCase(ddlAuto) && !"validate".equalsIgnoreCase(ddlAuto));
        tempEmf.setJpaVendorAdapter(vendorAdapter);

        Map<String, Object> props = new HashMap<>();
        props.put("hibernate.hbm2ddl.auto", ddlAuto);
        props.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        props.put("hibernate.physical_naming_strategy",
            "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy");
        if (config.getSchema() != null && !config.getSchema().isBlank()) {
          props.put("hibernate.default_schema", config.getSchema());
        }
        tempEmf.setJpaPropertyMap(props);

        tempEmf.afterPropertiesSet();
        EntityManagerFactory emf = tempEmf.getObject();
        if (emf != null) {
          emf.close();
        }
        ds.close();

        log.info("Schema initialized successfully for tenant: {}", tenantId);
      } catch (Exception e) {
        log.error("Failed to initialize schema for tenant: {}", tenantId, e);
      } finally {
        TenantContext.clear();
      }
    });
  }

  /**
   * Prod'da JPA_DDL_AUTO=validate iken ai_usage_logs tablosunun her tenant DB'de
   * mevcut olmasını garanti eder (idempotent CREATE TABLE IF NOT EXISTS).
   */
  private void ensureAiUsageLogsTable() {
    if (tenantProperties == null || tenantProperties.getDatasources() == null) {
      return;
    }
    final String ddl = """
        CREATE TABLE IF NOT EXISTS ai_usage_logs (
          id              BIGSERIAL PRIMARY KEY,
          created_at      TIMESTAMP(6),
          updated_at      TIMESTAMP(6),
          request_id      VARCHAR(36)  NOT NULL,
          step_order      INTEGER      NOT NULL DEFAULT 1,
          endpoint        VARCHAR(100) NOT NULL,
          action          VARCHAR(60)  NOT NULL,
          model           VARCHAR(60)  NOT NULL,
          username        VARCHAR(100),
          tenant_id       VARCHAR(60),
          user_input      TEXT,
          prompt_text     TEXT,
          response_text   TEXT,
          parsed_output   TEXT,
          prompt_chars    INTEGER      NOT NULL DEFAULT 0,
          response_chars  INTEGER      NOT NULL DEFAULT 0,
          input_tokens    INTEGER      NOT NULL DEFAULT 0,
          output_tokens   INTEGER      NOT NULL DEFAULT 0,
          thought_tokens  INTEGER      NOT NULL DEFAULT 0,
          cached_tokens   INTEGER      NOT NULL DEFAULT 0,
          total_tokens    INTEGER      NOT NULL DEFAULT 0,
          duration_ms     BIGINT       NOT NULL DEFAULT 0,
          status          VARCHAR(20)  NOT NULL,
          http_status     INTEGER,
          error_code      VARCHAR(60),
          error_message   TEXT
        );
        CREATE INDEX IF NOT EXISTS idx_ai_usage_logs_created_at ON ai_usage_logs (created_at);
        CREATE INDEX IF NOT EXISTS idx_ai_usage_logs_action     ON ai_usage_logs (action);
        CREATE INDEX IF NOT EXISTS idx_ai_usage_logs_status     ON ai_usage_logs (status);
        CREATE INDEX IF NOT EXISTS idx_ai_usage_logs_request_id ON ai_usage_logs (request_id);
        CREATE INDEX IF NOT EXISTS idx_ai_usage_logs_username   ON ai_usage_logs (username);
        CREATE TABLE IF NOT EXISTS ai_quota (
          tenant_id           VARCHAR(64) PRIMARY KEY,
          monthly_token_limit BIGINT,
          daily_request_limit BIGINT,
          updated_at          TIMESTAMP(6)
        );
        """;

    tenantProperties.getDatasources().forEach((tenantId, config) -> {
      try (java.sql.Connection conn = java.sql.DriverManager.getConnection(
              config.getUrl(), config.getUsername(), config.getPassword());
          java.sql.Statement stmt = conn.createStatement()) {
        if (config.getSchema() != null && !config.getSchema().isBlank()) {
          conn.setSchema(config.getSchema());
        }
        stmt.execute(ddl);
        log.info("Ensured ai_usage_logs table for tenant '{}'", tenantId);
      } catch (Exception e) {
        log.warn("Could not auto-ensure ai_usage_logs for tenant '{}': {}", tenantId, e.getMessage());
      }
    });
  }
}
