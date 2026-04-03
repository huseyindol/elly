package com.cms.config;

import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

import com.cms.config.DataSourceConfig.TenantDataSourceConfig;
import com.cms.config.DataSourceConfig.TenantDataSourceProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Tenant başına JavaMailSender çözümler.
 *
 * <p>Tenant konfigürasyonunda mailUsername set edilmişse o tenant'a özel
 * bir JavaMailSenderImpl oluşturur ve cache'ler.
 * Set edilmemişse global (Spring Boot auto-configured) sender'a düşer.
 *
 * <p>Cache key: tenantId. Tenant konfigürasyonu runtime'da değişmez,
 * dolayısıyla application restart'a kadar geçerlidir.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantMailSenderFactory {

  private final TenantDataSourceProperties tenantDataSourceProperties;
  /** Spring Boot'un auto-configure ettiği global JavaMailSender. */
  private final JavaMailSender globalMailSender;

  @Value("${spring.mail.host:localhost}")
  private String globalHost;

  @Value("${spring.mail.port:1025}")
  private int globalPort;

  private final ConcurrentHashMap<String, JavaMailSender> cache = new ConcurrentHashMap<>();

  /**
   * Verilen tenant için JavaMailSender döndürür.
   * Tenant config'inde SMTP bilgileri yoksa global sender kullanılır.
   */
  public JavaMailSender getMailSender(String tenantId) {
    TenantDataSourceConfig config = tenantDataSourceProperties.getDatasources().get(tenantId);

    if (config == null
        || config.getMailUsername() == null
        || config.getMailUsername().isBlank()) {
      log.debug("No SMTP config for tenant '{}', using global mail sender", tenantId);
      return globalMailSender;
    }

    return cache.computeIfAbsent(tenantId, id -> buildMailSender(id, config));
  }

  private JavaMailSender buildMailSender(String tenantId, TenantDataSourceConfig config) {
    String host = (config.getMailHost() != null && !config.getMailHost().isBlank())
        ? config.getMailHost()
        : globalHost;
    int port = (config.getMailPort() != null)
        ? config.getMailPort()
        : globalPort;

    JavaMailSenderImpl sender = new JavaMailSenderImpl();
    sender.setHost(host);
    sender.setPort(port);
    sender.setUsername(config.getMailUsername());
    sender.setPassword(config.getMailPassword());
    sender.setDefaultEncoding("UTF-8");

    Properties props = sender.getJavaMailProperties();
    props.put("mail.transport.protocol", "smtp");
    props.put("mail.smtp.auth", "true");
    props.put("mail.smtp.starttls.enable", "true");
    props.put("mail.smtp.starttls.required", "true");

    log.info("Built JavaMailSender for tenant '{}' → {}:{} user={}",
        tenantId, host, port, config.getMailUsername());
    return sender;
  }
}
