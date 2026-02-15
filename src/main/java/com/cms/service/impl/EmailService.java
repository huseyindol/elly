package com.cms.service.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import com.cms.config.RabbitMQConfig;
import com.cms.dto.DtoEmailLog;
import com.cms.dto.EmailRequest;
import com.cms.entity.EmailLog;
import com.cms.enums.EmailStatus;
import com.cms.repository.EmailLogRepository;
import com.cms.service.IEmailService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService implements IEmailService {

  private final EmailLogRepository emailLogRepository;
  private final RabbitTemplate rabbitTemplate;
  private final ObjectMapper objectMapper;

  @Override
  public DtoEmailLog sendEmail(EmailRequest request) {
    // 1. Convert dynamicData to JSON string
    String payloadJson = null;
    if (request.getDynamicData() != null && !request.getDynamicData().isEmpty()) {
      try {
        payloadJson = objectMapper.writeValueAsString(request.getDynamicData());
      } catch (JsonProcessingException e) {
        log.error("Failed to serialize dynamicData: {}", e.getMessage());
        throw new RuntimeException("Failed to serialize dynamic data", e);
      }
    }

    // 2. Save to DB with PENDING status
    EmailLog emailLog = new EmailLog();
    emailLog.setRecipient(request.getTo());
    emailLog.setSubject(request.getSubject());
    emailLog.setTemplateName(request.getTemplateName());
    emailLog.setPayloadJson(payloadJson);
    emailLog.setStatus(EmailStatus.PENDING);
    emailLog.setRetryCount(0);

    EmailLog saved = emailLogRepository.save(emailLog);
    log.info("Email log created with ID: {} for recipient: {}", saved.getId(), saved.getRecipient());

    // 3. Send ID to RabbitMQ queue
    rabbitTemplate.convertAndSend(
        RabbitMQConfig.EMAIL_EXCHANGE,
        RabbitMQConfig.EMAIL_ROUTING_KEY,
        saved.getId());
    log.info("Email log ID: {} sent to RabbitMQ queue", saved.getId());

    // 4. Return DTO
    return DtoEmailLog.builder()
        .id(saved.getId())
        .recipient(saved.getRecipient())
        .subject(saved.getSubject())
        .templateName(saved.getTemplateName())
        .status(saved.getStatus())
        .retryCount(saved.getRetryCount())
        .createdAt(saved.getCreatedAt())
        .sentAt(saved.getSentAt())
        .build();
  }

  @Override
  public List<String> getAvailableTemplates() {
    List<String> templates = new ArrayList<>();
    try {
      PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
      Resource[] resources = resolver.getResources("classpath:/templates/emails/*.html");
      for (Resource resource : resources) {
        String filename = resource.getFilename();
        if (filename != null && filename.endsWith(".html")) {
          templates.add(filename.replace(".html", ""));
        }
      }
    } catch (IOException e) {
      log.error("Failed to list email templates: {}", e.getMessage());
    }
    return templates;
  }
}
