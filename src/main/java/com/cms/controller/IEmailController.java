package com.cms.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.cms.dto.DtoEmailLog;
import com.cms.dto.EmailRequest;
import com.cms.entity.RootEntityResponse;
import com.cms.enums.EmailStatus;

public interface IEmailController {

  RootEntityResponse<DtoEmailLog> sendEmail(EmailRequest request);

  RootEntityResponse<List<String>> getAvailableTemplates();

  RootEntityResponse<DtoEmailLog> retry(Long id);

  RootEntityResponse<Page<DtoEmailLog>> list(EmailStatus status, Pageable pageable);
}
