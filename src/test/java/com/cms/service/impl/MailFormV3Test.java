package com.cms.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import com.cms.config.TenantMailSenderFactory;
import com.cms.dto.DtoEmailLog;
import com.cms.dto.DtoEmailStats;
import com.cms.dto.EmailMessage;
import com.cms.dto.EmailRequest;
import com.cms.entity.EmailLog;
import com.cms.entity.FormDefinition;
import com.cms.entity.FormSubmission;
import com.cms.entity.MailAccount;
import com.cms.entity.form.FieldDefinition;
import com.cms.entity.form.FormSchema;
import com.cms.enums.EmailStatus;
import com.cms.exception.FormValidationException;
import com.cms.exception.TooManyRequestsException;
import com.cms.exception.ValidationException;
import com.cms.repository.EmailLogRepository;
import com.cms.repository.FormDefinitionRepository;
import com.cms.repository.FormSubmissionRepository;
import com.cms.service.IEmailService;
import com.cms.service.IFormDefinitionService;
import com.cms.service.IMailAccountService;
import com.cms.service.INotificationService;
import com.cms.service.form.ConditionEvaluator;
import com.fasterxml.jackson.databind.ObjectMapper;

class MailFormV3Test {

  @Test
  void b1_formDefinitionServiceValidatesCcAndBccFormats() {
    FormDefinitionRepository defRepo = mock(FormDefinitionRepository.class);
    FormSubmissionRepository subRepo = mock(FormSubmissionRepository.class);
    FormDefinitionService service = new FormDefinitionService(defRepo, subRepo, new ObjectMapper());

    MailAccount account = new MailAccount();
    ReflectionTestUtils.setField(account, "id", 1L);
    account.setActive(true);

    FormDefinition form = new FormDefinition();
    form.setTitle("Iletisim");
    form.setNotificationEnabled(true);
    form.setSenderMailAccount(account);
    form.setRecipientEmail("to1@example.com, to2@example.com");
    form.setCcEmails("not-an-email");

    assertThrows(ValidationException.class, () -> service.save(form));

    form.setCcEmails("cc1@example.com, cc2@example.com");
    form.setBccEmails("bcc@example.com");
    when(defRepo.save(any(FormDefinition.class))).thenAnswer(inv -> inv.getArgument(0));

    FormDefinition saved = service.save(form);
    assertEquals("cc1@example.com, cc2@example.com", saved.getCcEmails());
    assertEquals("bcc@example.com", saved.getBccEmails());
  }

  @Test
  void b2_emailQuotaServiceTracksDailyLimitAndMarksQuotaExceeded() {
    EmailQuotaService quotaService = new EmailQuotaService(null);
    assertFalse(quotaService.isQuotaExceeded("tenant1", 10L, 2));
    assertEquals(1L, quotaService.incrementDailySentCount("tenant1", 10L));
    assertEquals(2L, quotaService.incrementDailySentCount("tenant1", 10L));
    assertTrue(quotaService.isQuotaExceeded("tenant1", 10L, 2));

    EmailLogRepository logRepo = mock(EmailLogRepository.class);
    TenantMailSenderFactory senderFactory = mock(TenantMailSenderFactory.class);
    EmailTemplateRenderer renderer = mock(EmailTemplateRenderer.class);
    RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    INotificationService notificationService = mock(INotificationService.class);

    EmailQueueService queueService = new EmailQueueService(
        logRepo, senderFactory, renderer, new ObjectMapper(), rabbitTemplate, notificationService, quotaService);

    MailAccount account = new MailAccount();
    ReflectionTestUtils.setField(account, "id", 10L);
    account.setName("Primary SMTP");
    account.setTenantId("tenant1");
    account.setActive(true);
    account.setDailyLimit(2);

    EmailLog log = new EmailLog();
    ReflectionTestUtils.setField(log, "id", 99L);
    log.setStatus(EmailStatus.PENDING);
    log.setMailAccount(account);
    log.setRecipient("user@example.com");
    log.setSubject("Subj");
    log.setTemplateName("form-notification");

    when(logRepo.findByIdWithMailAccount(99L)).thenReturn(Optional.of(log));

    queueService.processEmailMessage(new EmailMessage("tenant1", 99L));

    assertEquals(EmailStatus.QUOTA_EXCEEDED, log.getStatus());
    assertNotNull(log.getErrorMessage());
    verify(senderFactory, never()).getMailSender(any());
    verify(logRepo).save(log);
  }

  @Test
  void b3_spamProtectionBlocksHoneypotAndRateLimitsByIp() {
    FormSpamProtectionService spamService = new FormSpamProtectionService(null);
    spamService.setMaxSubmitsPerMinute(2);

    assertThrows(FormValidationException.class,
        () -> spamService.validateTopLevelHoneypot("http://spam.bot"));

    Map<String, Object> payloadWithHp = new HashMap<>();
    payloadWithHp.put("name", "Ali");
    payloadWithHp.put("_hp", "bot-filled");
    assertThrows(FormValidationException.class,
        () -> spamService.verifyAndSanitizeSubmission(1L, payloadWithHp));

    Map<String, Object> cleanPayload = new HashMap<>();
    cleanPayload.put("name", "Ali");
    cleanPayload.put("_hp", "");
    spamService.verifyAndSanitizeSubmission(1L, cleanPayload);
    assertFalse(cleanPayload.containsKey("_hp"));

    spamService.checkRateLimit("tenant1", 5L, "203.0.113.10");
    spamService.checkRateLimit("tenant1", 5L, "203.0.113.10");
    assertThrows(TooManyRequestsException.class,
        () -> spamService.checkRateLimit("tenant1", 5L, "203.0.113.10"));
  }

  @Test
  void b4_emailServiceReturnsStatsAndPersistsCcBcc() {
    EmailLogRepository logRepo = mock(EmailLogRepository.class);
    RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    IMailAccountService mailAccountService = mock(IMailAccountService.class);
    EmailService emailService = new EmailService(logRepo, rabbitTemplate, new ObjectMapper(), mailAccountService);

    when(logRepo.count()).thenReturn(25L);
    when(logRepo.countByStatus(EmailStatus.SENT)).thenReturn(20L);
    when(logRepo.countByStatus(EmailStatus.PENDING)).thenReturn(2L);
    when(logRepo.countByStatus(EmailStatus.FAILED)).thenReturn(2L);
    when(logRepo.countByStatus(EmailStatus.QUOTA_EXCEEDED)).thenReturn(1L);
    when(logRepo.countByStatusAndCreatedAtAfter(eq(EmailStatus.FAILED), any(Date.class))).thenReturn(1L);
    when(logRepo.countByStatusAndCreatedAtAfter(eq(EmailStatus.QUOTA_EXCEEDED), any(Date.class))).thenReturn(1L);

    DtoEmailStats stats = emailService.getStats();
    assertEquals(25L, stats.getTotalCount());
    assertEquals(20L, stats.getSentCount());
    assertEquals(2L, stats.getPendingCount());
    assertEquals(2L, stats.getFailedCount());
    assertEquals(1L, stats.getQuotaExceededCount());
    assertEquals(2L, stats.getFailedLast24hCount());

    MailAccount account = new MailAccount();
    ReflectionTestUtils.setField(account, "id", 3L);
    account.setActive(true);
    account.setFromAddress("noreply@example.com");
    when(mailAccountService.getEntityById(3L)).thenReturn(account);
    when(logRepo.save(any(EmailLog.class))).thenAnswer(inv -> {
      EmailLog el = inv.getArgument(0);
      ReflectionTestUtils.setField(el, "id", 55L);
      return el;
    });

    EmailRequest req = new EmailRequest(
        "to@example.com",
        "cc@example.com",
        "bcc@example.com",
        "Konu",
        "custom-tpl",
        Map.of("k", "v"),
        3L);
    DtoEmailLog dto = emailService.sendEmail(req);
    assertEquals("cc@example.com", dto.getCc());
    assertEquals("bcc@example.com", dto.getBcc());
  }

  @Test
  void b6_formSubmissionUsesCustomTemplateKeyAndCcBcc() {
    FormSubmissionRepository subRepo = mock(FormSubmissionRepository.class);
    IFormDefinitionService defService = mock(IFormDefinitionService.class);
    ConditionEvaluator condEval = mock(ConditionEvaluator.class);
    IEmailService emailService = mock(IEmailService.class);
    INotificationService notificationService = mock(INotificationService.class);
    FormSpamProtectionService spamService = new FormSpamProtectionService(null);

    FormSubmissionService submissionService = new FormSubmissionService(
        subRepo, defService, condEval, List.of(), emailService, notificationService, spamService);

    MailAccount sender = new MailAccount();
    ReflectionTestUtils.setField(sender, "id", 7L);
    sender.setActive(true);

    FieldDefinition nameField = new FieldDefinition();
    nameField.setId("fullName");
    nameField.setLabel("Ad Soyad");
    nameField.setType("text");
    nameField.setRequired(true);

    FormSchema schema = new FormSchema();
    schema.setFields(List.of(nameField));

    FormDefinition def = new FormDefinition();
    ReflectionTestUtils.setField(def, "id", 12L);
    def.setTitle("Basvuru Formu");
    def.setVersion(1);
    def.setActive(true);
    def.setSchema(schema);
    def.setNotificationEnabled(true);
    def.setSenderMailAccount(sender);
    def.setRecipientEmail("hr@example.com");
    def.setCcEmails("manager@example.com");
    def.setBccEmails("audit@example.com");
    def.setNotificationTemplateKey("hr-application-template");

    when(defService.getById(12L)).thenReturn(def);
    when(condEval.shouldBeVisible(any(), any())).thenReturn(true);
    when(subRepo.save(any(FormSubmission.class))).thenAnswer(inv -> {
      FormSubmission s = inv.getArgument(0);
      ReflectionTestUtils.setField(s, "id", 101L);
      return s;
    });

    submissionService.submitForm(12L, Map.of("fullName", "Ayse Demir"));

    ArgumentCaptor<EmailRequest> captor = ArgumentCaptor.forClass(EmailRequest.class);
    verify(emailService).sendEmail(captor.capture());

    EmailRequest captured = captor.getValue();
    assertEquals("hr@example.com", captured.getTo());
    assertEquals("manager@example.com", captured.getCc());
    assertEquals("audit@example.com", captured.getBcc());
    assertEquals("hr-application-template", captured.getTemplateName());
    assertEquals("Ayse Demir", captured.getDynamicData().get("fullName"));
  }
}
