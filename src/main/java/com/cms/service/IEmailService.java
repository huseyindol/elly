package com.cms.service;

import java.util.List;

import com.cms.dto.DtoEmailLog;
import com.cms.dto.EmailRequest;

public interface IEmailService {

  DtoEmailLog sendEmail(EmailRequest request);

  List<String> getAvailableTemplates();
}
