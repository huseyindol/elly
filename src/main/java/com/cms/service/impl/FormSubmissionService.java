package com.cms.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.DtoFormSubmissionCleanupResult;
import com.cms.dto.DtoFormSubmissionExport;
import com.cms.dto.EmailRequest;
import com.cms.entity.FormDefinition;
import com.cms.entity.FormSubmission;
import com.cms.entity.MailAccount;
import com.cms.entity.form.FieldDefinition;
import com.cms.entity.form.FormSchema;
import com.cms.exception.FormValidationException;
import com.cms.exception.ResourceNotFoundException;
import com.cms.exception.ValidationException;
import com.cms.repository.FormSubmissionRepository;
import com.cms.config.TenantContext;
import com.cms.enums.NotificationType;
import com.cms.service.IEmailService;
import com.cms.service.IFormDefinitionService;
import com.cms.service.IFormSubmissionService;
import com.cms.service.INotificationService;
import com.cms.service.form.ConditionEvaluator;
import com.cms.service.form.FieldValidator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * FormSubmission akisi:
 * <ol>
 *   <li>Form aktif mi + schema gecerli mi kontrolu</li>
 *   <li>Conditional visibility + payload sanitization</li>
 *   <li>Alan-bazli validation</li>
 *   <li>Kaydet</li>
 *   <li>Mail+Form v1: notificationEnabled ise mail tetikle (try-catch — submission
 *       her halukarda commit olur)</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FormSubmissionService implements IFormSubmissionService {

  private static final String NOTIFICATION_TEMPLATE = "form-notification";
  private static final DateTimeFormatter SUBMITTED_AT_FORMATTER =
      DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

  private final FormSubmissionRepository formSubmissionRepository;
  private final IFormDefinitionService formDefinitionService;
  private final ConditionEvaluator conditionEvaluator;
  private final List<FieldValidator> fieldValidators;
  private final IEmailService emailService;
  private final INotificationService notificationService;
  private final FormSpamProtectionService formSpamProtectionService;

  @Override
  @Transactional
  @CacheEvict(value = "formSubmissions", allEntries = true)
  public FormSubmission submitForm(Long formDefinitionId, Map<String, Object> payload) {
    // Create a mutable copy of the payload for sanitization
    Map<String, Object> sanitizedPayload = new HashMap<>(payload != null ? payload : new HashMap<>());

    // Step 0 (B3): Spam protection (honeypot keys + IP rate limit)
    if (formSpamProtectionService != null) {
      formSpamProtectionService.verifyAndSanitizeSubmission(formDefinitionId, sanitizedPayload);
    }

    // Step 1: Fetch the FormDefinition
    FormDefinition formDefinition = formDefinitionService.getById(formDefinitionId);

    if (!Boolean.TRUE.equals(formDefinition.getActive())) {
      throw new FormValidationException("Bu form şu anda aktif değil");
    }

    FormSchema schema = formDefinition.getSchema();
    if (schema == null || schema.getFields() == null || schema.getFields().isEmpty()) {
      throw new FormValidationException("Form şeması geçersiz veya alan tanımı bulunamadı");
    }

    // Step 1b: Sender availability (Mail+Form v1) — bildirim acik ise profil gecerli olmali
    if (Boolean.TRUE.equals(formDefinition.getNotificationEnabled())) {
      validateNotificationConfig(formDefinition);
    }

    // Step 2 & 3: Iterate through fields and evaluate conditions
    for (FieldDefinition field : schema.getFields()) {
      String fieldId = field.getId();

      // Evaluate if field should be visible
      boolean isVisible = conditionEvaluator.shouldBeVisible(field.getCondition(), sanitizedPayload);

      if (!isVisible) {
        // Field is not visible - remove from payload (sanitization)
        sanitizedPayload.remove(fieldId);
      } else {
        // Field is visible - validate it
        Object value = sanitizedPayload.get(fieldId);
        validateField(value, field);
      }
    }

    // Step 4: All validations passed, save the submission
    FormSubmission submission = new FormSubmission();
    submission.setFormDefinition(formDefinition);
    submission.setPayload(sanitizedPayload);
    FormSubmission saved = formSubmissionRepository.save(submission);

    // Step 5: Trigger mail notification (best-effort — submission commit olmali)
    if (Boolean.TRUE.equals(formDefinition.getNotificationEnabled())) {
      triggerNotification(formDefinition, saved);
    }

    triggerInAppNotification(formDefinition, saved);

    return saved;
  }

  /**
   * notificationEnabled=true ise: sender mail hesabi atanmis ve aktif olmali;
   * recipientEmail bos olmamali. Aksi halde 422 — submit bastan basarisiz olur.
   * (v2: SMTP kredansiyeli DB'de; sender valid oldugu surece yollanabilir.)
   */
  private void validateNotificationConfig(FormDefinition formDefinition) {
    MailAccount sender = formDefinition.getSenderMailAccount();
    if (sender == null) {
      throw new ValidationException("Form bildirimi acik fakat senderMailAccount atanmamis");
    }
    if (!Boolean.TRUE.equals(sender.getActive())) {
      throw new ValidationException(
          "Secilen mail hesabi aktif degil (id=" + sender.getId() + ")");
    }
    if (formDefinition.getRecipientEmail() == null || formDefinition.getRecipientEmail().isBlank()) {
      throw new ValidationException("recipientEmail bos olamaz");
    }
  }

  private void triggerInAppNotification(FormDefinition formDefinition, FormSubmission submission) {
    try {
      String tenantId = TenantContext.getTenantId();
      Map<String, Object> metadata = new LinkedHashMap<>();
      metadata.put("formId", formDefinition.getId());
      metadata.put("submissionId", submission.getId());
      if (tenantId != null) {
        metadata.put("tenantId", tenantId);
      }

      notificationService.notifyAdminPlusUsers(
          NotificationType.FORM_SUBMISSION,
          "Yeni form gonderimi",
          formDefinition.getTitle() + " formuna yeni gonderim",
          "/forms/" + formDefinition.getId() + "/submissions",
          tenantId,
          metadata);
    } catch (Exception ex) {
      log.error("Form in-app bildirimi tetiklenemedi: formId={}, submissionId={}, hata={}",
          formDefinition.getId(), submission.getId(), ex.getMessage(), ex);
    }
  }

  /**
   * Mail gondermeyi tetikler. Hata olursa submission commit'ini etkilemez —
   * EmailLog + RabbitMQ retry mekanizmasi devreye girer.
   */
  private void triggerNotification(FormDefinition formDefinition, FormSubmission submission) {
    try {
      MailAccount sender = formDefinition.getSenderMailAccount();

      Map<String, Object> dynamicData = new LinkedHashMap<>();
      dynamicData.put("formTitle", formDefinition.getTitle());
      dynamicData.put("formVersion", formDefinition.getVersion());
      dynamicData.put("submissionId", submission.getId());
      dynamicData.put("submittedAt", submission.getSubmittedAt() != null
          ? SUBMITTED_AT_FORMATTER.format(submission.getSubmittedAt())
          : "");
      dynamicData.put("payload", submission.getPayload());
      if (submission.getPayload() != null) {
        submission.getPayload().forEach(dynamicData::putIfAbsent);
      }

      String subject = (formDefinition.getNotificationSubject() != null
          && !formDefinition.getNotificationSubject().isBlank())
              ? formDefinition.getNotificationSubject()
              : "Yeni form gonderimi: " + formDefinition.getTitle();

      String templateKey = (formDefinition.getNotificationTemplateKey() != null
          && !formDefinition.getNotificationTemplateKey().isBlank())
              ? formDefinition.getNotificationTemplateKey().trim()
              : NOTIFICATION_TEMPLATE;

      EmailRequest request = new EmailRequest(
          formDefinition.getRecipientEmail(),
          formDefinition.getCcEmails(),
          formDefinition.getBccEmails(),
          subject,
          templateKey,
          dynamicData,
          sender.getId());

      emailService.sendEmail(request);
      log.info("Form bildirim maili kuyruklandi: formId={}, submissionId={}, senderMailAccount={}, template={}, to={}",
          formDefinition.getId(), submission.getId(), sender.getId(), templateKey, formDefinition.getRecipientEmail());
    } catch (Exception ex) {
      // Submission kaydi rollback olmasin; mail tekrar denenebilir / admin inceleyebilir.
      log.error("Form bildirim maili tetiklenemedi: formId={}, submissionId={}, hata={}",
          formDefinition.getId(), submission.getId(), ex.getMessage(), ex);
    }
  }

  private void validateField(Object value, FieldDefinition fieldDef) {
    String fieldType = fieldDef.getType();

    // Find the appropriate validator
    FieldValidator validator = fieldValidators.stream()
        .filter(v -> v.supports(fieldType))
        .findFirst()
        .orElse(null);

    if (validator != null) {
      validator.validate(value, fieldDef);
    } else {
      // No validator found for this type - only check required
      if (Boolean.TRUE.equals(fieldDef.getRequired()) &&
          (value == null || value.toString().trim().isEmpty())) {
        throw new FormValidationException(fieldDef.getId(),
            String.format("'%s' alanı zorunludur", fieldDef.getLabel()));
      }
    }
  }

  @Override
  @Cacheable(value = "formSubmissions", key = "#id")
  public FormSubmission getById(Long id) {
    return formSubmissionRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("FormSubmission", id));
  }

  @Override
  @Cacheable(value = "formSubmissions", key = "'byFormId_' + #formDefinitionId")
  public List<FormSubmission> getByFormDefinitionId(Long formDefinitionId) {
    return formSubmissionRepository.findByFormIdOrderBySubmittedAtDesc(formDefinitionId);
  }

  @Override
  @Cacheable(value = "formSubmissions", key = "'byFormIdPaged_' + #formDefinitionId + '_' + #pageable.pageNumber + '_' + #pageable.pageSize + '_' + #pageable.sort.toString()")
  public Page<FormSubmission> getByFormDefinitionIdPaged(Long formDefinitionId, Pageable pageable) {
    return formSubmissionRepository.findByFormDefinitionId(formDefinitionId, pageable);
  }

  @Override
  @Cacheable(value = "formSubmissions", key = "'countByFormId_' + #formDefinitionId")
  public Long countByFormId(Long formDefinitionId) {
    return formSubmissionRepository.countByFormId(formDefinitionId);
  }

  @Override
  @Transactional
  @CacheEvict(value = "formSubmissions", allEntries = true)
  public Boolean delete(Long id) {
    if (formSubmissionRepository.existsById(id)) {
      formSubmissionRepository.deleteById(id);
      return true;
    }
    return false;
  }

  @Override
  @Transactional(readOnly = true)
  public DtoFormSubmissionExport exportCsv(Long formDefinitionId, LocalDate from, LocalDate to) {
    FormDefinition formDefinition = formDefinitionService.getById(formDefinitionId);
    List<FormSubmission> submissions;
    if (from != null || to != null) {
      LocalDateTime start = from != null ? from.atStartOfDay() : LocalDateTime.of(1970, 1, 1, 0, 0);
      LocalDateTime end = to != null ? to.atTime(23, 59, 59) : LocalDateTime.now().plusDays(1);
      submissions = formSubmissionRepository.findByFormIdAndSubmittedAtBetweenOrderBySubmittedAtDesc(
          formDefinitionId, start, end);
    } else {
      submissions = formSubmissionRepository.findByFormIdOrderBySubmittedAtDesc(formDefinitionId);
    }

    Map<String, String> fieldLabels = new LinkedHashMap<>();
    if (formDefinition.getSchema() != null && formDefinition.getSchema().getFields() != null) {
      for (FieldDefinition f : formDefinition.getSchema().getFields()) {
        if (f != null && f.getId() != null) {
          fieldLabels.put(f.getId(), f.getLabel() != null && !f.getLabel().isBlank() ? f.getLabel() : f.getId());
        }
      }
    }

    Set<String> extraKeys = new LinkedHashSet<>();
    for (FormSubmission sub : submissions) {
      if (sub.getPayload() != null) {
        for (String key : sub.getPayload().keySet()) {
          if (!fieldLabels.containsKey(key)) {
            extraKeys.add(key);
          }
        }
      }
    }

    List<String> orderedKeys = new ArrayList<>(fieldLabels.keySet());
    orderedKeys.addAll(extraKeys);

    StringBuilder sb = new StringBuilder();
    sb.append('\uFEFF'); // UTF-8 BOM for Excel compatibility
    sb.append("ID,Gönderim Tarihi");
    for (String key : orderedKeys) {
      sb.append(',').append(escapeCsv(fieldLabels.getOrDefault(key, key)));
    }
    sb.append("\r\n");

    for (FormSubmission sub : submissions) {
      sb.append(sub.getId() != null ? sub.getId() : "");
      sb.append(',');
      sb.append(escapeCsv(sub.getSubmittedAt() != null
          ? SUBMITTED_AT_FORMATTER.format(sub.getSubmittedAt())
          : ""));
      Map<String, Object> payload = sub.getPayload();
      for (String key : orderedKeys) {
        Object val = payload != null ? payload.get(key) : null;
        sb.append(',').append(escapeCsv(val != null ? String.valueOf(val) : ""));
      }
      sb.append("\r\n");
    }

    String safeTitle = formDefinition.getTitle() != null
        ? formDefinition.getTitle().replaceAll("[^a-zA-Z0-9_-]", "_").toLowerCase()
        : "form_" + formDefinitionId;
    String fileName = safeTitle + "_submissions.csv";
    return new DtoFormSubmissionExport(fileName, submissions.size(), sb.toString());
  }

  @Override
  @Transactional
  @CacheEvict(value = "formSubmissions", allEntries = true)
  public DtoFormSubmissionCleanupResult cleanupOldSubmissions(Long formDefinitionId, int retentionDays) {
    if (retentionDays < 1) {
      throw new ValidationException("Saklama suresi (retentionDays) en az 1 gun olmalidir");
    }
    formDefinitionService.getById(formDefinitionId);
    LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
    int deleted = formSubmissionRepository.deleteByFormIdAndSubmittedAtBefore(formDefinitionId, cutoff);
    return new DtoFormSubmissionCleanupResult(deleted, retentionDays);
  }

  @Override
  @Transactional
  @CacheEvict(value = "formSubmissions", allEntries = true)
  public int cleanupAllExpiredSubmissions(int retentionDays) {
    if (retentionDays < 1) {
      return 0;
    }
    LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
    return formSubmissionRepository.deleteBySubmittedAtBefore(cutoff);
  }

  private String escapeCsv(String raw) {
    if (raw == null) {
      return "";
    }
    boolean needsQuotes = raw.contains(",") || raw.contains("\"") || raw.contains("\n") || raw.contains("\r");
    String escaped = raw.replace("\"", "\"\"");
    return needsQuotes ? "\"" + escaped + "\"" : escaped;
  }
}
