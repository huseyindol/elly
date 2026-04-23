package com.cms.service;

import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.cms.dto.DtoEmailTemplate;
import com.cms.dto.DtoEmailTemplateIU;
import com.cms.dto.DtoEmailTemplatePreviewResponse;
import com.cms.entity.EmailTemplate;

public interface IEmailTemplateService {

  Page<DtoEmailTemplate> list(Pageable pageable);

  DtoEmailTemplate getByKey(String key);

  DtoEmailTemplate create(DtoEmailTemplateIU request);

  DtoEmailTemplate update(String key, DtoEmailTemplateIU request);

  void delete(String key);

  DtoEmailTemplatePreviewResponse preview(String key, Map<String, Object> data);

  boolean existsByKey(String tenantId, String key);

  void createGlobal(String key, String subject, String htmlBody, String description);

  /**
   * EmailTemplateRenderer tarafından çağrılır.
   * Tenant-specific template varsa onu döner, yoksa global (tenantId=null) template'i döner.
   * Her ikisi de yoksa null döner — renderer classpath'e düşer.
   */
  EmailTemplate loadTemplate(String tenantId, String templateKey);
}
