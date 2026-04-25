package com.cms.service.impl;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.cms.config.RabbitMQConfig;
import com.cms.config.TenantContext;
import com.cms.config.TenantMailSenderFactory;
import com.cms.dto.EmailMessage;
import com.cms.entity.EmailLog;
import com.cms.entity.MailAccount;
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
  private final EmailTemplateRenderer templateRenderer;
  private final ObjectMapper objectMapper;
  private final RabbitTemplate rabbitTemplate;

  private static final int MAX_RETRY_COUNT = 3;

  @RabbitListener(queues = RabbitMQConfig.EMAIL_QUEUE)
  @Override
  public void processEmailMessage(EmailMessage message) {
    // Consumer thread'inde TenantContext set edilmesi ZORUNLU (multitenant-routing skill).
    TenantContext.setTenantId(message.getTenantId());
    try {
      Long emailLogId = message.getEmailLogId();
      log.info("Mail isleniyor: id={}, tenant={}", emailLogId, message.getTenantId());

      // Consumer thread'inde session kisa omurlu — mailAccount lazy proxy'sini
      // ayni sorguda init et. Aksi halde LazyInitializationException (no session).
      EmailLog emailLog = emailLogRepository.findByIdWithMailAccount(emailLogId).orElse(null);
      if (emailLog == null) {
        log.error("EmailLog bulunamadi: id={}, tenant={}", emailLogId, message.getTenantId());
        return;
      }

      if (emailLog.getStatus() == EmailStatus.SENT || emailLog.getStatus() == EmailStatus.FAILED) {
        log.info("EmailLog zaten islenmis: id={}, status={}", emailLogId, emailLog.getStatus());
        return;
      }

      MailAccount account = emailLog.getMailAccount();
      if (account == null) {
        // Varsayilan hesap kaldirildi — mailAccountId zorunlu.
        emailLog.setStatus(EmailStatus.FAILED);
        emailLog.setErrorMessage("mailAccountId zorunludur (varsayilan hesap kaldirildi)");
        emailLogRepository.save(emailLog);
        log.error("EmailLog mailAccountId olmadan kuyruga girmis: id={}", emailLogId);
        return;
      }

      try {
        // 1. MailAccount aktiflik kontrolu
        if (!Boolean.TRUE.equals(account.getActive())) {
          throw new IllegalStateException(
              "MailAccount pasif: id=" + account.getId() + ", name=" + account.getName());
        }

        // 2. Template degiskenlerini hazirla
        Map<String, Object> dynamicData = new HashMap<>();
        if (emailLog.getPayloadJson() != null && !emailLog.getPayloadJson().isEmpty()) {
          dynamicData = objectMapper.readValue(
              emailLog.getPayloadJson(), new TypeReference<Map<String, Object>>() {});
        }

        // 3. DB-first renderer ile render et (DB'de yoksa classpath fallback)
        EmailTemplateRenderer.RenderedEmail rendered =
            templateRenderer.render(emailLog.getTemplateName(), dynamicData);
        String htmlContent = rendered.html();

        // 4. Sender'i fabrikadan al ve gonder (v2: DB-based)
        JavaMailSender sender = mailSenderFactory.getMailSender(account);
        sendHtmlEmail(sender, account.getFromAddress(), emailLog.getRecipient(),
            emailLog.getSubject(), htmlContent);

        // 5. Basari -> SENT
        emailLog.setStatus(EmailStatus.SENT);
        emailLog.setSentAt(new Date());
        emailLogRepository.save(emailLog);
        log.info("Mail gonderildi: logId={}, mailAccountId={}, from={}, to={}",
            emailLogId, account.getId(), account.getFromAddress(), emailLog.getRecipient());

      } catch (Exception e) {
        log.error("Mail gonderilemedi: logId={}, hata={}", emailLogId, e.getMessage());

        emailLog.setRetryCount(emailLog.getRetryCount() + 1);
        emailLog.setErrorMessage(truncate(e.getMessage()));

        if (emailLog.getRetryCount() > MAX_RETRY_COUNT) {
          emailLog.setStatus(EmailStatus.FAILED);
          log.error("Mail FAILED durumuna alindi: logId={}, retryCount={}",
              emailLogId, emailLog.getRetryCount());
        } else {
          log.info("Mail yeniden kuyruga aliniyor: logId={}, deneme={}/{}",
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
    if (to != null && to.contains(",")) {
      String[] addresses = Arrays.stream(to.split(","))
          .map(String::trim)
          .filter(s -> !s.isEmpty())
          .toArray(String[]::new);
      helper.setTo(addresses);
    } else {
      helper.setTo(to);
    }
    helper.setSubject(subject);
    helper.setText(htmlContent, true);
    sender.send(mime);
  }

  private String truncate(String s) {
    if (s == null) return null;
    return s.length() > 1000 ? s.substring(0, 1000) + "..." : s;
  }
}
