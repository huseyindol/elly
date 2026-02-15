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

import com.cms.config.RabbitMQConfig;
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
  private final JavaMailSender javaMailSender;
  private final TemplateEngine templateEngine;
  private final ObjectMapper objectMapper;
  private final RabbitTemplate rabbitTemplate;

  private static final int MAX_RETRY_COUNT = 3;

  @Value("${mail.from:noreply@elly.com}")
  private String mailFrom;

  @RabbitListener(queues = RabbitMQConfig.EMAIL_QUEUE)
  @Override
  public void processEmailMessage(Long emailLogId) {
    log.info("Received email log ID from queue: {}", emailLogId);

    EmailLog emailLog = emailLogRepository.findById(emailLogId).orElse(null);
    if (emailLog == null) {
      log.error("EmailLog not found for ID: {}", emailLogId);
      return;
    }

    // Skip if already SENT or FAILED
    if (emailLog.getStatus() == EmailStatus.SENT || emailLog.getStatus() == EmailStatus.FAILED) {
      log.info("EmailLog ID: {} already processed with status: {}", emailLogId, emailLog.getStatus());
      return;
    }

    try {
      // 1. Deserialize payloadJson to Map
      Map<String, Object> dynamicData = new HashMap<>();
      if (emailLog.getPayloadJson() != null && !emailLog.getPayloadJson().isEmpty()) {
        dynamicData = objectMapper.readValue(emailLog.getPayloadJson(),
            new TypeReference<Map<String, Object>>() {
            });
      }

      // 2. Create Thymeleaf Context and render template
      Context context = new Context();
      context.setVariables(dynamicData);
      String htmlContent = templateEngine.process("emails/" + emailLog.getTemplateName(), context);

      // 3. Send email via SMTP
      sendHtmlEmail(emailLog.getRecipient(), emailLog.getSubject(), htmlContent);

      // 4. Update status to SENT
      emailLog.setStatus(EmailStatus.SENT);
      emailLog.setSentAt(new Date());
      emailLogRepository.save(emailLog);
      log.info("Email sent successfully for ID: {}", emailLogId);

    } catch (Exception e) {
      log.error("Failed to send email for ID: {}: {}", emailLogId, e.getMessage());

      emailLog.setRetryCount(emailLog.getRetryCount() + 1);
      emailLog.setErrorMessage(e.getMessage());

      if (emailLog.getRetryCount() > MAX_RETRY_COUNT) {
        emailLog.setStatus(EmailStatus.FAILED);
        log.error("Email ID: {} marked as FAILED after {} retries", emailLogId, emailLog.getRetryCount());
      } else {
        log.info("Email ID: {} will be retried. Retry count: {}/{}", emailLogId,
            emailLog.getRetryCount(), MAX_RETRY_COUNT);
        // Re-queue for retry
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.EMAIL_EXCHANGE,
            RabbitMQConfig.EMAIL_ROUTING_KEY,
            emailLogId);
      }

      emailLogRepository.save(emailLog);
    }
  }

  private void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException {
    MimeMessage mimeMessage = javaMailSender.createMimeMessage();
    MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
    helper.setFrom(mailFrom);
    helper.setTo(to);
    helper.setSubject(subject);
    helper.setText(htmlContent, true);
    javaMailSender.send(mimeMessage);
  }
}
