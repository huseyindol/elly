package com.cms.service.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.cms.config.DataSourceConfig.TenantDataSourceConfig;
import com.cms.config.DataSourceConfig.TenantDataSourceProperties;
import com.cms.config.RabbitMQConfig;
import com.cms.config.TenantContext;
import com.cms.config.TenantMailSenderFactory;
import com.cms.dto.EmailMessage;
import com.cms.entity.EmailLog;
import com.cms.enums.EmailStatus;
import com.cms.repository.EmailLogRepository;
import com.cms.service.IEmailQueueService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailQueueService implements IEmailQueueService {

  private final EmailLogRepository emailLogRepository;
  private final TenantMailSenderFactory mailSenderFactory;
  private final TenantDataSourceProperties tenantDataSourceProperties;
  private final TemplateEngine templateEngine;
  private final ObjectMapper objectMapper;
  private final RabbitTemplate rabbitTemplate;

  private static final int MAX_RETRY_COUNT = 3;

  /** Global fallback — tenant'a özgü mailFrom set edilmezse kullanılır. */
  @Value("${mail.from:noreply@elly.com}")
  private String defaultMailFrom;

  @RabbitListener(queues = RabbitMQConfig.EMAIL_QUEUE)
  @Override
  public void processEmailMessage(EmailMessage message) {
    // Async listener: HTTP request scope'u dışında çalışır.
    // Tenant context'i mesajdan alarak set et.
    TenantContext.setTenantId(message.getTenantId());
    try {
      Long emailLogId = message.getEmailLogId();
      log.info("Received email log ID: {} for tenant: {}", emailLogId, message.getTenantId());

      EmailLog emailLog = emailLogRepository.findById(emailLogId).orElse(null);
      if (emailLog == null) {
        log.error("EmailLog not found for ID: {} on tenant: {}", emailLogId, message.getTenantId());
        return;
      }

      // Zaten işlenmiş kayıtları atla
      if (emailLog.getStatus() == EmailStatus.SENT || emailLog.getStatus() == EmailStatus.FAILED) {
        log.info("EmailLog ID: {} already processed with status: {}", emailLogId, emailLog.getStatus());
        return;
      }

      try {
        // 1. payloadJson'ı Map'e deserialize et
        Map<String, Object> dynamicData = new HashMap<>();
        if (emailLog.getPayloadJson() != null && !emailLog.getPayloadJson().isEmpty()) {
          dynamicData = objectMapper.readValue(emailLog.getPayloadJson(),
              new TypeReference<Map<String, Object>>() {
              });
        }

        // 2. Thymeleaf template render et
        Context context = new Context();
        context.setVariables(dynamicData);
        String htmlContent = templateEngine.process("emails/" + emailLog.getTemplateName(), context);

        // 3. Tenant'a özgü from adresi ve mail sender'ı belirle
        String fromAddress = resolveMailFrom(message.getTenantId());
        JavaMailSender mailSender = mailSenderFactory.getMailSender(message.getTenantId());

        // 4. SMTP üzerinden gönder
        sendHtmlEmail(mailSender, emailLog.getRecipient(), emailLog.getSubject(), htmlContent, fromAddress);

        // 5. SENT olarak güncelle
        emailLog.setStatus(EmailStatus.SENT);
        emailLog.setSentAt(new Date());
        emailLogRepository.save(emailLog);
        log.info("Email sent successfully from '{}' for ID: {} on tenant: {}",
            fromAddress, emailLogId, message.getTenantId());

      } catch (Exception e) {
        log.error("Failed to send email for ID: {} on tenant: {}: {}", emailLogId, message.getTenantId(), e.getMessage());

        emailLog.setRetryCount(emailLog.getRetryCount() + 1);
        emailLog.setErrorMessage(e.getMessage());

        if (emailLog.getRetryCount() > MAX_RETRY_COUNT) {
          emailLog.setStatus(EmailStatus.FAILED);
          log.error("Email ID: {} on tenant: {} marked as FAILED after {} retries",
              emailLogId, message.getTenantId(), emailLog.getRetryCount());
        } else {
          log.info("Email ID: {} will be retried. Retry count: {}/{}",
              emailLogId, emailLog.getRetryCount(), MAX_RETRY_COUNT);
          rabbitTemplate.convertAndSend(
              RabbitMQConfig.EMAIL_EXCHANGE,
              RabbitMQConfig.EMAIL_ROUTING_KEY,
              message);
        }

        emailLogRepository.save(emailLog);
      }
    } finally {
      // Memory leak'i önle: ThreadLocal'ı temizle
      TenantContext.clear();
    }
  }

  /**
   * Tenant'a özgü mail-from adresini döndürür.
   * Tenant konfigürasyonunda set edilmemişse global {@code mail.from} değerine düşer.
   */
  private String resolveMailFrom(String tenantId) {
    TenantDataSourceConfig tenantConfig = tenantDataSourceProperties.getDatasources().get(tenantId);
    if (tenantConfig != null
        && tenantConfig.getMailFrom() != null
        && !tenantConfig.getMailFrom().isBlank()) {
      return tenantConfig.getMailFrom();
    }
    return defaultMailFrom;
  }

  private void sendHtmlEmail(JavaMailSender mailSender, String to, String subject,
      String htmlContent, String from) throws MessagingException {
    MimeMessage mimeMessage = mailSender.createMimeMessage();
    MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
    helper.setFrom(from);
    helper.setTo(to);
    helper.setSubject(subject);
    helper.setText(htmlContent, true);
    mailSender.send(mimeMessage);
  }
}
