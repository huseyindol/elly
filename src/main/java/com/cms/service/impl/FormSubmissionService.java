package com.cms.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.entity.FormDefinition;
import com.cms.entity.FormSubmission;
import com.cms.entity.form.FieldDefinition;
import com.cms.entity.form.FormSchema;
import com.cms.exception.FormValidationException;
import com.cms.exception.ResourceNotFoundException;
import com.cms.repository.FormSubmissionRepository;
import com.cms.service.IFormDefinitionService;
import com.cms.service.IFormSubmissionService;
import com.cms.service.form.ConditionEvaluator;
import com.cms.service.form.FieldValidator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FormSubmissionService implements IFormSubmissionService {

  private final FormSubmissionRepository formSubmissionRepository;
  private final IFormDefinitionService formDefinitionService;
  private final ConditionEvaluator conditionEvaluator;
  private final List<FieldValidator> fieldValidators;

  @Override
  @Transactional
  @CacheEvict(value = "formSubmissions", allEntries = true)
  public FormSubmission submitForm(Long formDefinitionId, Map<String, Object> payload) {
    // Step 1: Fetch the FormDefinition
    FormDefinition formDefinition = formDefinitionService.getById(formDefinitionId);

    if (!Boolean.TRUE.equals(formDefinition.getActive())) {
      throw new FormValidationException("Bu form şu anda aktif değil");
    }

    FormSchema schema = formDefinition.getSchema();
    if (schema == null || schema.getFields() == null || schema.getFields().isEmpty()) {
      throw new FormValidationException("Form şeması geçersiz veya alan tanımı bulunamadı");
    }

    // Create a mutable copy of the payload for sanitization
    Map<String, Object> sanitizedPayload = new HashMap<>(payload != null ? payload : new HashMap<>());

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

    return formSubmissionRepository.save(submission);
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
}
