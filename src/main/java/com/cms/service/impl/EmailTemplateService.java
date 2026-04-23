package com.cms.service.impl;

import java.util.Map;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.config.TenantContext;
import com.cms.dto.DtoEmailTemplate;
import com.cms.dto.DtoEmailTemplateIU;
import com.cms.dto.DtoEmailTemplatePreviewResponse;
import com.cms.entity.EmailTemplate;
import com.cms.exception.ResourceNotFoundException;
import com.cms.exception.ValidationException;
import com.cms.repository.EmailTemplateRepository;
import com.cms.service.IEmailTemplateService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailTemplateService implements IEmailTemplateService {

  private final EmailTemplateRepository templateRepository;
  private final EmailTemplateRenderer renderer;

  @Override
  @Transactional(readOnly = true)
  public Page<DtoEmailTemplate> list(Pageable pageable) {
    String tenantId = TenantContext.getTenantId();
    return templateRepository.findByTenantIdOrTenantIdIsNull(tenantId, pageable)
        .map(this::toDto);
  }

  @Override
  @Transactional(readOnly = true)
  public DtoEmailTemplate getByKey(String key) {
    return toDto(resolveTemplate(key));
  }

  @Override
  @Transactional
  @CacheEvict(value = "emailTemplates", allEntries = true)
  public DtoEmailTemplate create(DtoEmailTemplateIU request) {
    String tenantId = TenantContext.getTenantId();

    if (templateRepository.existsByTenantIdAndTemplateKey(tenantId, request.getTemplateKey())) {
      throw new ValidationException(
          "Bu tenant için '" + request.getTemplateKey() + "' key'li template zaten mevcut");
    }

    EmailTemplate entity = new EmailTemplate();
    entity.setTenantId(tenantId);
    applyRequest(entity, request);
    EmailTemplate saved = templateRepository.save(entity);
    log.info("EmailTemplate oluşturuldu: key={}, tenant={}", saved.getTemplateKey(), tenantId);
    return toDto(saved);
  }

  @Override
  @Transactional
  @CacheEvict(value = "emailTemplates", allEntries = true)
  public DtoEmailTemplate update(String key, DtoEmailTemplateIU request) {
    EmailTemplate entity = resolveTemplate(key);
    applyRequest(entity, request);
    EmailTemplate saved = templateRepository.save(entity);
    log.info("EmailTemplate güncellendi: key={}, id={}", key, saved.getId());
    return toDto(saved);
  }

  @Override
  @Transactional
  @CacheEvict(value = "emailTemplates", allEntries = true)
  public void delete(String key) {
    EmailTemplate entity = resolveTemplate(key);
    templateRepository.delete(entity);
    log.info("EmailTemplate silindi: key={}, id={}", key, entity.getId());
  }

  @Override
  @Transactional(readOnly = true)
  public DtoEmailTemplatePreviewResponse preview(String key, Map<String, Object> data) {
    EmailTemplate entity = resolveTemplate(key);
    EmailTemplateRenderer.RenderedEmail rendered = renderer.renderFromEntity(entity, data);
    return DtoEmailTemplatePreviewResponse.builder()
        .html(rendered.html())
        .subject(rendered.subject())
        .build();
  }

  @Override
  @Transactional(readOnly = true)
  public boolean existsByKey(String tenantId, String key) {
    if (tenantId == null) {
      return templateRepository.existsByTenantIdIsNullAndTemplateKey(key);
    }
    return templateRepository.existsByTenantIdAndTemplateKey(tenantId, key);
  }

  @Override
  @Transactional
  public void createGlobal(String key, String subject, String htmlBody, String description) {
    if (templateRepository.existsByTenantIdIsNullAndTemplateKey(key)) {
      log.debug("Global template zaten mevcut, seed atlanıyor: key={}", key);
      return;
    }
    EmailTemplate entity = new EmailTemplate();
    entity.setTenantId(null);
    entity.setTemplateKey(key);
    entity.setSubject(subject);
    entity.setHtmlBody(htmlBody);
    entity.setDescription(description);
    entity.setActive(true);
    entity.setVersion(1);
    templateRepository.save(entity);
    log.info("Global EmailTemplate seed edildi: key={}", key);
  }

  @Override
  @Transactional(readOnly = true)
  @Cacheable(value = "emailTemplates", key = "#templateKey")
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

  // ==================== HELPERS ====================

  /**
   * Mevcut tenant için template'i bulur — önce tenant-specific, yoksa global.
   */
  private EmailTemplate resolveTemplate(String key) {
    String tenantId = TenantContext.getTenantId();
    if (tenantId != null) {
      EmailTemplate specific = templateRepository
          .findByTenantIdAndTemplateKey(tenantId, key)
          .orElse(null);
      if (specific != null) return specific;
    }
    return templateRepository
        .findByTenantIdIsNullAndTemplateKey(key)
        .orElseThrow(() -> new ResourceNotFoundException("EmailTemplate '" + key + "' bulunamadı"));
  }

  private void applyRequest(EmailTemplate entity, DtoEmailTemplateIU req) {
    entity.setTemplateKey(req.getTemplateKey());
    entity.setSubject(req.getSubject());
    entity.setHtmlBody(req.getHtmlBody());
    entity.setDescription(req.getDescription());
    entity.setActive(req.getActive() != null ? req.getActive() : true);
    if (req.getVersion() != null) {
      entity.setVersion(req.getVersion());
    }
  }

  private DtoEmailTemplate toDto(EmailTemplate e) {
    return DtoEmailTemplate.builder()
        .id(e.getId())
        .tenantId(e.getTenantId())
        .templateKey(e.getTemplateKey())
        .subject(e.getSubject())
        .htmlBody(e.getHtmlBody())
        .description(e.getDescription())
        .active(e.getActive())
        .version(e.getVersion())
        .optimisticLockVersion(e.getOptimisticLockVersion())
        .createdAt(e.getCreatedAt())
        .updatedAt(e.getUpdatedAt())
        .build();
  }
}
