package com.cms.service;

import com.cms.dto.EmailMessage;

public interface IEmailQueueService {

  void processEmailMessage(EmailMessage message);
}
