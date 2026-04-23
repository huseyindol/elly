package com.cms.service.impl;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.entity.EmailTemplate;
import com.cms.repository.EmailTemplateRepository;

import lombok.RequiredArgsConstructor;

/**
 * EmailTemplateRenderer'ın bağımlılık döngüsüne girmeden kullanabileceği
 * ince cache katmanı. Yalnızca okuma yapar — CRUD EmailTemplateService'te.
 *
 * Cache key tenant prefix'i CacheConfig.computePrefixWith() ile otomatik eklenir.
 */
@Service
@RequiredArgsConstructor
public class EmailTemplateLookupService {

  private final EmailTemplateRepository templateRepository;

  /**
   * Tenant-specific template varsa onu döner, yoksa global (tenantId=null) template'i döner.
   * Her ikisi de yoksa null döner — renderer classpath'e düşer.
   */
  @Cacheable(value = "emailTemplates", key = "#templateKey")
  @Transactional(readOnly = true)
  public EmailTemplate loadTemplate(String tenantId, String templateKey) {
    if (tenantId != null) {
      EmailTemplate specific = templateRepository
          .findByTenantIdAndTemplateKey(tenantId, templateKey)
          .orElse(null);
      if (specific != null && Boolean.TRUE.equals(specific.getActive())) {
        return specific;
      }
    }
    return templateRepository
        .findByTenantIdIsNullAndTemplateKey(templateKey)
        .filter(t -> Boolean.TRUE.equals(t.getActive()))
        .orElse(null);
  }
}
