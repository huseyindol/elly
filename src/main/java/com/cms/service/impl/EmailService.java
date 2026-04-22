package com.cms.service.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import com.cms.config.RabbitMQConfig;
import com.cms.config.TenantContext;
import com.cms.dto.DtoEmailLog;
import com.cms.dto.EmailMessage;
import com.cms.dto.EmailRequest;
import com.cms.entity.EmailLog;
import com.cms.entity.MailAccount;
import com.cms.enums.EmailStatus;
import com.cms.exception.ValidationException;
import com.cms.repository.EmailLogRepository;
import com.cms.service.IEmailService;
import com.cms.service.IMailAccountService;
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
  private final IMailAccountService mailAccountService;

  @Override
  public DtoEmailLog sendEmail(EmailRequest request) {
    if (request.getMailAccountId() == null) {
      throw new ValidationException("mailAccountId zorunludur — varsayilan hesap kaldirildi");
    }

    MailAccount mailAccount = mailAccountService.getEntityById(request.getMailAccountId());
    if (!Boolean.TRUE.equals(mailAccount.getActive())) {
      throw new ValidationException(
          "Secilen mail hesabi aktif degil (id=" + mailAccount.getId() + ")");
    }

    // 1. dynamicData'yi JSON string'e cevir
    String payloadJson = null;
    if (request.getDynamicData() != null && !request.getDynamicData().isEmpty()) {
      try {
        payloadJson = objectMapper.writeValueAsString(request.getDynamicData());
      } catch (JsonProcessingException e) {
        log.error("Failed to serialize dynamicData: {}", e.getMessage());
        throw new ValidationException("Dinamik veri JSON'a cevrilemedi: " + e.getMessage(), e);
      }
    }

    // 2. PENDING status ile DB'ye kaydet
    EmailLog emailLog = new EmailLog();
    emailLog.setRecipient(request.getTo());
    emailLog.setSubject(request.getSubject());
    emailLog.setTemplateName(request.getTemplateName());
    emailLog.setPayloadJson(payloadJson);
    emailLog.setStatus(EmailStatus.PENDING);
    emailLog.setRetryCount(0);
    emailLog.setMailAccount(mailAccount);

    EmailLog saved = emailLogRepository.save(emailLog);
    log.info("Email log created: id={}, recipient={}, tenant={}, mailAccountId={}, from={}",
        saved.getId(), saved.getRecipient(), TenantContext.getTenantId(),
        mailAccount.getId(), mailAccount.getFromAddress());

    // 3. Tenant ID'yi mesajla birlikte RabbitMQ queue'ya gonder
    String tenantId = TenantContext.getTenantId();
    rabbitTemplate.convertAndSend(
        RabbitMQConfig.EMAIL_EXCHANGE,
        RabbitMQConfig.EMAIL_ROUTING_KEY,
        new EmailMessage(tenantId, saved.getId()));

    // 4. DTO dondur
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
