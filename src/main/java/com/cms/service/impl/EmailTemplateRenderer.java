package com.cms.service.impl;

import java.util.Collections;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import com.cms.config.TenantContext;
import com.cms.entity.EmailTemplate;

import lombok.extern.slf4j.Slf4j;

/**
 * DB-first, classpath-fallback Thymeleaf render bileşeni.
 *
 * Render önceliği:
 * 1. DB'de tenant-specific template (IEmailTemplateService.loadTemplate)
 * 2. DB'de global template (tenantId=null)
 * 3. Classpath: templates/emails/{key}.html (önceki davranış korunur)
 *
 * Classpath fallback sayesinde v4'e geçiş sırasında mevcut template dosyaları
 * çalışmaya devam eder — zero-downtime migration.
 */
@Slf4j
@Component
public class EmailTemplateRenderer {

  private final EmailTemplateLookupService lookupService;
  private final TemplateEngine classpathEngine;
  private final TemplateEngine stringHtmlEngine;
  private final TemplateEngine stringTextEngine;

  public EmailTemplateRenderer(EmailTemplateLookupService lookupService,
      TemplateEngine classpathEngine) {
    this.lookupService = lookupService;
    this.classpathEngine = classpathEngine;
    this.stringHtmlEngine = buildStringEngine(TemplateMode.HTML);
    this.stringTextEngine = buildStringEngine(TemplateMode.TEXT);
  }

  /**
   * EmailQueueService consumer tarafından çağrılır.
   * TenantContext set edilmiş olmalıdır.
   */
  public RenderedEmail render(String templateKey, Map<String, Object> variables) {
    String tenantId = TenantContext.getTenantId();
    EmailTemplate template = lookupService.loadTemplate(tenantId, templateKey);

    if (template != null) {
      log.debug("EmailTemplate DB'den render ediliyor: key={}, tenant={}", templateKey, tenantId);
      return renderFromEntity(template, variables);
    }

    // Classpath fallback: eski davranış
    log.debug("EmailTemplate classpath fallback: key={}, tenant={}", templateKey, tenantId);
    Context ctx = buildContext(variables);
    String html = classpathEngine.process("emails/" + templateKey, ctx);
    return new RenderedEmail(html, null);
  }

  /**
   * Preview endpoint'i için: entity doğrudan verilir (DB lookup yapılmaz).
   */
  public RenderedEmail renderFromEntity(EmailTemplate template, Map<String, Object> data) {
    Map<String, Object> vars = data != null ? data : Collections.emptyMap();
    Context ctx = buildContext(vars);
    String html = stringHtmlEngine.process(template.getHtmlBody(), ctx);
    String subject = renderSubject(template.getSubject(), vars);
    return new RenderedEmail(html, subject);
  }

  private String renderSubject(String subjectTemplate, Map<String, Object> vars) {
    if (subjectTemplate == null || subjectTemplate.isBlank()) {
      return subjectTemplate;
    }
    try {
      Context ctx = buildContext(vars);
      return stringTextEngine.process(subjectTemplate, ctx);
    } catch (Exception e) {
      log.warn("Subject render başarısız, ham değer döndürülüyor: {}", e.getMessage());
      return subjectTemplate;
    }
  }

  private static Context buildContext(Map<String, Object> variables) {
    Context ctx = new Context();
    if (variables != null) {
      ctx.setVariables(variables);
    }
    return ctx;
  }

  private static TemplateEngine buildStringEngine(TemplateMode mode) {
    StringTemplateResolver resolver = new StringTemplateResolver();
    resolver.setTemplateMode(mode);
    resolver.setCacheable(false);
    TemplateEngine engine = new TemplateEngine();
    engine.setTemplateResolver(resolver);
    return engine;
  }

  public record RenderedEmail(String html, String subject) {}
}
