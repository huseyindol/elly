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
@EnableJpaRepositories(basePackages = "com.cms.repository")
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
}
