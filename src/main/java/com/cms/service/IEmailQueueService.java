package com.cms.service;

public interface IEmailQueueService {

  void processEmailMessage(Long emailLogId);
}
