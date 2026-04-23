package com.cms.service.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.config.RabbitMQConfig;
import com.cms.config.TenantContext;
import com.cms.dto.DtoEmailLog;
import com.cms.dto.EmailMessage;
import com.cms.dto.EmailRequest;
import com.cms.entity.EmailLog;
import com.cms.entity.MailAccount;
import com.cms.enums.EmailStatus;
import com.cms.exception.ResourceNotFoundException;
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
    return toDto(saved);
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

  @Override
  @Transactional
  public DtoEmailLog retry(Long emailLogId) {
    if (emailLogId == null) {
      throw new ValidationException("emailLogId zorunludur");
    }

    // JOIN FETCH ile cek — consumer ayni pattern'i kullaniyor, simetrik olsun
    EmailLog emailLog = emailLogRepository.findByIdWithMailAccount(emailLogId)
        .orElseThrow(() -> new ResourceNotFoundException("EmailLog", emailLogId));

    // SENT kayitlari tekrar gonderilmez — idempotency ve spam onlemi
    if (emailLog.getStatus() == EmailStatus.SENT) {
      throw new ValidationException(
          "SENT durumundaki mail yeniden gonderilemez (id=" + emailLogId + ")");
    }

    MailAccount account = emailLog.getMailAccount();
    if (account == null) {
      throw new ValidationException(
          "EmailLog mailAccount'suz kaydedilmis, retry imkansiz (id=" + emailLogId + ")");
    }
    if (!Boolean.TRUE.equals(account.getActive())) {
      throw new ValidationException(
          "MailAccount pasif, retry imkansiz (accountId=" + account.getId() + ")");
    }

    // Reset: PENDING + retryCount=0 + errorMessage=null, sentAt bosaltilmaz
    // (eski basarisiz denemenin zamani kaldirilmasin istersen not dusurulebilir)
    emailLog.setStatus(EmailStatus.PENDING);
    emailLog.setRetryCount(0);
    emailLog.setErrorMessage(null);
    EmailLog saved = emailLogRepository.save(emailLog);

    // Queue'ya tekrar publish — tenant context istek anindaki context'ten
    String tenantId = TenantContext.getTenantId();
    rabbitTemplate.convertAndSend(
        RabbitMQConfig.EMAIL_EXCHANGE,
        RabbitMQConfig.EMAIL_ROUTING_KEY,
        new EmailMessage(tenantId, saved.getId()));

    log.info("Email retry queued: id={}, tenant={}, recipient={}, mailAccountId={}",
        saved.getId(), tenantId, saved.getRecipient(), account.getId());

    return toDto(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<DtoEmailLog> list(EmailStatus status, Pageable pageable) {
    Page<EmailLog> page = (status == null)
        ? emailLogRepository.findAll(pageable)
        : emailLogRepository.findByStatus(status, pageable);
    return page.map(this::toDto);
  }

  /**
   * EmailLog -> DtoEmailLog. MailAccount lazy field'ina dokunmaz, sadece
   * scalar alanlari kullanir — Page.map icinde session kapanmis olsa bile guvenli.
   */
  private DtoEmailLog toDto(EmailLog entity) {
    return DtoEmailLog.builder()
        .id(entity.getId())
        .recipient(entity.getRecipient())
        .subject(entity.getSubject())
        .templateName(entity.getTemplateName())
        .status(entity.getStatus())
        .retryCount(entity.getRetryCount())
        .createdAt(entity.getCreatedAt())
        .sentAt(entity.getSentAt())
        .build();
  }
}
