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
    seedInline("welcome", "Hoş Geldin, [[${userName}]]!", buildWelcomeTemplate(),
        "Yeni kullanıcı kayıt hoşgeldin maili");
    seedInline("password-reset", "Şifre Sıfırlama Talebiniz",
        buildPasswordResetTemplate(), "Şifre sıfırlama bağlantısı maili");
  }

  private void seedInline(String key, String subject, String htmlBody, String description) {
    String originalTenant = TenantContext.getTenantId();
    try {
      TenantContext.setTenantId("basedb");
      if (emailTemplateService.existsByKey(null, key)) {
        log.debug("EmailTemplate seed atlanıyor (zaten mevcut): key={}", key);
        return;
      }
      emailTemplateService.createGlobal(key, subject, htmlBody, description);
    } finally {
      TenantContext.setTenantId(originalTenant);
    }
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

  private static String buildWelcomeTemplate() {
    return """
        <!DOCTYPE html>
        <html xmlns:th="http://www.thymeleaf.org">
        <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <title>Hoş Geldiniz</title>
          <style>
            body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 0; background-color: #f4f7fa; }
            .container { max-width: 600px; margin: 32px auto; background: #ffffff; border-radius: 10px; overflow: hidden; box-shadow: 0 2px 12px rgba(0,0,0,0.08); }
            .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 40px 32px; text-align: center; }
            .header h1 { color: #ffffff; margin: 0; font-size: 26px; font-weight: 700; }
            .header p { color: rgba(255,255,255,0.85); margin: 8px 0 0; font-size: 15px; }
            .body { padding: 36px 32px; }
            .body p { color: #444; line-height: 1.7; font-size: 15px; margin: 0 0 16px; }
            .btn { display: inline-block; background: #667eea; color: #ffffff; text-decoration: none; padding: 14px 32px; border-radius: 6px; font-weight: 600; font-size: 15px; margin: 8px 0 24px; }
            .divider { border: none; border-top: 1px solid #eee; margin: 24px 0; }
            .footer { text-align: center; padding: 20px 32px; font-size: 12px; color: #999; background: #f9f9f9; }
          </style>
        </head>
        <body>
          <div class="container">
            <div class="header">
              <h1>Hoş Geldiniz! 🎉</h1>
              <p>Hesabınız başarıyla oluşturuldu</p>
            </div>
            <div class="body">
              <p>Merhaba <strong th:text="${userName}">Kullanıcı</strong>,</p>
              <p>Platformumuza hoş geldiniz! Hesabınız başarıyla oluşturuldu. Hemen giriş yaparak tüm özellikleri keşfedebilirsiniz.</p>
              <p style="text-align:center;">
                <a class="btn" th:href="${dashboardUrl}" href="#">Panele Git</a>
              </p>
              <hr class="divider"/>
              <p style="font-size:13px; color:#888;">Hesabınızı siz oluşturmadıysanız bu e-postayı görmezden gelebilirsiniz.</p>
            </div>
            <div class="footer">
              Bu e-posta otomatik olarak gönderilmiştir, lütfen yanıtlamayınız.
            </div>
          </div>
        </body>
        </html>
        """;
  }

  private static String buildPasswordResetTemplate() {
    return """
        <!DOCTYPE html>
        <html xmlns:th="http://www.thymeleaf.org">
        <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <title>Şifre Sıfırlama</title>
          <style>
            body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 0; background-color: #f4f7fa; }
            .container { max-width: 600px; margin: 32px auto; background: #ffffff; border-radius: 10px; overflow: hidden; box-shadow: 0 2px 12px rgba(0,0,0,0.08); }
            .header { background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); padding: 40px 32px; text-align: center; }
            .header h1 { color: #ffffff; margin: 0; font-size: 24px; font-weight: 700; }
            .header p { color: rgba(255,255,255,0.85); margin: 8px 0 0; font-size: 14px; }
            .body { padding: 36px 32px; }
            .body p { color: #444; line-height: 1.7; font-size: 15px; margin: 0 0 16px; }
            .btn { display: inline-block; background: #f5576c; color: #ffffff; text-decoration: none; padding: 14px 32px; border-radius: 6px; font-weight: 600; font-size: 15px; margin: 8px 0 24px; }
            .code-box { background: #f8f9fa; border: 1px solid #e9ecef; border-radius: 6px; padding: 16px 24px; text-align: center; font-size: 28px; font-weight: 700; letter-spacing: 6px; color: #333; margin: 16px 0 24px; }
            .warning { background: #fff8e1; border-left: 4px solid #ffc107; padding: 12px 16px; border-radius: 4px; font-size: 13px; color: #856404; }
            .divider { border: none; border-top: 1px solid #eee; margin: 24px 0; }
            .footer { text-align: center; padding: 20px 32px; font-size: 12px; color: #999; background: #f9f9f9; }
          </style>
        </head>
        <body>
          <div class="container">
            <div class="header">
              <h1>🔐 Şifre Sıfırlama</h1>
              <p>Şifre sıfırlama talebiniz alındı</p>
            </div>
            <div class="body">
              <p>Merhaba <strong th:text="${userName}">Kullanıcı</strong>,</p>
              <p>Şifre sıfırlama talebinde bulundunuz. Aşağıdaki bağlantıya tıklayarak yeni şifrenizi belirleyebilirsiniz:</p>
              <p style="text-align:center;">
                <a class="btn" th:href="${resetUrl}" href="#">Şifremi Sıfırla</a>
              </p>
              <p>Ya da aşağıdaki kodu kullanın:</p>
              <div class="code-box" th:text="${resetCode}">123456</div>
              <div class="warning">
                ⚠️ Bu bağlantı <strong th:text="${expiresIn}">30 dakika</strong> içinde geçerliliğini yitirecektir.
              </div>
              <hr class="divider"/>
              <p style="font-size:13px; color:#888;">Bu talebi siz yapmadıysanız şifreniz değişmeyecektir. Yine de güvenliğiniz için lütfen destek ekibiyle iletişime geçin.</p>
            </div>
            <div class="footer">
              Bu e-posta otomatik olarak gönderilmiştir, lütfen yanıtlamayınız.
            </div>
          </div>
        </body>
        </html>
        """;
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
