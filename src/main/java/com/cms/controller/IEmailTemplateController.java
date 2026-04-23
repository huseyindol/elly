package com.cms.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.cms.dto.DtoEmailTemplate;
import com.cms.dto.DtoEmailTemplateIU;
import com.cms.dto.DtoEmailTemplatePreviewRequest;
import com.cms.dto.DtoEmailTemplatePreviewResponse;
import com.cms.entity.RootEntityResponse;

public interface IEmailTemplateController {

  RootEntityResponse<Page<DtoEmailTemplate>> list(Pageable pageable);

  RootEntityResponse<DtoEmailTemplate> getByKey(String key);

  RootEntityResponse<DtoEmailTemplate> create(DtoEmailTemplateIU request);

  RootEntityResponse<DtoEmailTemplate> update(String key, DtoEmailTemplateIU request);

  RootEntityResponse<Void> delete(String key);

  RootEntityResponse<DtoEmailTemplatePreviewResponse> preview(String key,
      DtoEmailTemplatePreviewRequest request);
}
