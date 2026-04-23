package com.cms.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import com.cms.service.IEmailTemplateService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Uygulama başlangıcında mevcut classpath email template'lerini DB'ye seed eder.
 * Sadece global (tenantId=null) ve henüz DB'de olmayan template'ler eklenir — idempotent.
 *
 * DataInitializer'dan (@Order(1)) sonra çalışır (@Order(2)).
 * Bu sayede v3'ten v4'e zero-downtime geçiş sağlanır: DB'de template yokken
 * classpath fallback devreye girer, seed sonrası DB'den okunur.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(2)
public class EmailTemplateBootstrapRunner implements ApplicationRunner {

  private final IEmailTemplateService emailTemplateService;

  @Override
  public void run(ApplicationArguments args) {
    seed("form-notification", "Yeni Form Gönderimi: [[${formTitle}]]",
        "form-notification.html", "Form submit bildirim maili");
  }

  private void seed(String key, String subject, String filename, String description) {
    String originalTenant = TenantContext.getTenantId();
    try {
      TenantContext.setTenantId("basedb");

      if (emailTemplateService.existsByKey(null, key)) {
        log.debug("EmailTemplate seed atlanıyor (zaten mevcut): key={}", key);
        return;
      }

      String htmlBody = loadClasspathTemplate(filename);
      if (htmlBody == null) {
        log.warn("Classpath template bulunamadı, seed atlanıyor: {}", filename);
        return;
      }

      emailTemplateService.createGlobal(key, subject, htmlBody, description);

    } finally {
      TenantContext.setTenantId(originalTenant);
    }
  }

  private String loadClasspathTemplate(String filename) {
    try {
      ClassPathResource resource = new ClassPathResource("templates/emails/" + filename);
      return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      log.error("Classpath template okunamadı: {}, hata: {}", filename, e.getMessage());
      return null;
    }
  }
}
