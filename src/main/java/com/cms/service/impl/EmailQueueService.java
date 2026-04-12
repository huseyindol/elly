package com.cms.service.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.cms.config.RabbitMQConfig;
import com.cms.config.TenantContext;
import com.cms.config.TenantMailSenderFactory;
import com.cms.dto.EmailMessage;
import com.cms.entity.EmailLog;
import com.cms.entity.MailAccount;
import com.cms.enums.EmailStatus;
import com.cms.repository.EmailLogRepository;
import com.cms.service.IEmailQueueService;
import com.cms.service.IMailAccountService;
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
  private final IMailAccountService mailAccountService;
  private final TemplateEngine templateEngine;
  private final ObjectMapper objectMapper;
  private final RabbitTemplate rabbitTemplate;

  private static final int MAX_RETRY_COUNT = 3;

  @RabbitListener(queues = RabbitMQConfig.EMAIL_QUEUE)
  @Override
  public void processEmailMessage(EmailMessage message) {
    TenantContext.setTenantId(message.getTenantId());
    try {
      Long emailLogId = message.getEmailLogId();
      log.info("Mail işleniyor: id={}, tenant={}", emailLogId, message.getTenantId());

      EmailLog emailLog = emailLogRepository.findById(emailLogId).orElse(null);
      if (emailLog == null) {
        log.error("EmailLog bulunamadı: id={}, tenant={}", emailLogId, message.getTenantId());
        return;
      }

      if (emailLog.getStatus() == EmailStatus.SENT || emailLog.getStatus() == EmailStatus.FAILED) {
        log.info("EmailLog zaten işlenmiş: id={}, status={}", emailLogId, emailLog.getStatus());
        return;
      }

      try {
        // 1. Template değişkenlerini hazırla
        Map<String, Object> dynamicData = new HashMap<>();
        if (emailLog.getPayloadJson() != null && !emailLog.getPayloadJson().isEmpty()) {
          dynamicData = objectMapper.readValue(
              emailLog.getPayloadJson(), new TypeReference<Map<String, Object>>() {});
        }

        // 2. Thymeleaf ile render et
        Context ctx = new Context();
        ctx.setVariables(dynamicData);
        String htmlContent = templateEngine.process("emails/" + emailLog.getTemplateName(), ctx);

        // 3. Mail hesabını çöz: log'da kayıtlıysa onu kullan, yoksa varsayılanı al
        MailAccount account = emailLog.getMailAccount() != null
            ? emailLog.getMailAccount()
            : mailAccountService.getDefaultEntity();

        // 4. Sender'ı fabrikadan al ve gönder
        JavaMailSender sender = mailSenderFactory.getMailSender(account);
        sendHtmlEmail(sender, account.getFromAddress(), emailLog.getRecipient(),
            emailLog.getSubject(), htmlContent);

        // 5. Başarı → SENT
        emailLog.setStatus(EmailStatus.SENT);
        emailLog.setSentAt(new Date());
        emailLog.setMailAccount(account);
        emailLogRepository.save(emailLog);
        log.info("Mail gönderildi: logId={}, from={}, to={}", emailLogId,
            account.getFromAddress(), emailLog.getRecipient());

      } catch (Exception e) {
        log.error("Mail gönderilemedi: logId={}, hata={}", emailLogId, e.getMessage());

        emailLog.setRetryCount(emailLog.getRetryCount() + 1);
        emailLog.setErrorMessage(e.getMessage());

        if (emailLog.getRetryCount() > MAX_RETRY_COUNT) {
          emailLog.setStatus(EmailStatus.FAILED);
          log.error("Mail FAILED durumuna alındı: logId={}, retryCount={}",
              emailLogId, emailLog.getRetryCount());
        } else {
          log.info("Mail yeniden kuyruğa alınıyor: logId={}, deneme={}/{}",
              emailLogId, emailLog.getRetryCount(), MAX_RETRY_COUNT);
          rabbitTemplate.convertAndSend(
              RabbitMQConfig.EMAIL_RETRY_EXCHANGE,
              RabbitMQConfig.EMAIL_ROUTING_KEY,
              message);
        }

        emailLogRepository.save(emailLog);
      }
    } finally {
      TenantContext.clear();
    }
  }

  private void sendHtmlEmail(JavaMailSender sender, String from, String to,
      String subject, String htmlContent) throws MessagingException {
    MimeMessage mime = sender.createMimeMessage();
    MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");
    helper.setFrom(from);
    helper.setTo(to);
    helper.setSubject(subject);
    helper.setText(htmlContent, true);
    sender.send(mime);
  }
}
