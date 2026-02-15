package com.cms.controller;

import java.util.List;

import com.cms.dto.DtoEmailLog;
import com.cms.dto.EmailRequest;
import com.cms.entity.RootEntityResponse;

public interface IEmailController {

  RootEntityResponse<DtoEmailLog> sendEmail(EmailRequest request);

  RootEntityResponse<List<String>> getAvailableTemplates();
}
