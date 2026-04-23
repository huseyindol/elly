package com.cms.service;

import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.cms.dto.DtoEmailTemplate;
import com.cms.dto.DtoEmailTemplateIU;
import com.cms.dto.DtoEmailTemplatePreviewResponse;

public interface IEmailTemplateService {

  Page<DtoEmailTemplate> list(Pageable pageable);

  DtoEmailTemplate getByKey(String key);

  DtoEmailTemplate create(DtoEmailTemplateIU request);

  DtoEmailTemplate update(String key, DtoEmailTemplateIU request);

  void delete(String key);

  DtoEmailTemplatePreviewResponse preview(String key, Map<String, Object> data);

  boolean existsByKey(String tenantId, String key);

  void createGlobal(String key, String subject, String htmlBody, String description);
}
