package com.cms.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.cms.dto.DtoFormSubmissionCleanupResult;
import com.cms.dto.DtoFormSubmissionExport;
import com.cms.entity.FormSubmission;

public interface IFormSubmissionService {

  /**
   * Submit a form with validation.
   * This method handles the complete submission flow:
   * 1. Fetch the FormDefinition
   * 2. Evaluate conditional visibility for each field
   * 3. Sanitize the payload (remove hidden fields)
   * 4. Validate visible fields
   * 5. Save the submission
   */
  FormSubmission submitForm(Long formDefinitionId, Map<String, Object> payload);

  FormSubmission getById(Long id);

  List<FormSubmission> getByFormDefinitionId(Long formDefinitionId);

  Page<FormSubmission> getByFormDefinitionIdPaged(Long formDefinitionId, Pageable pageable);

  Long countByFormId(Long formDefinitionId);

  Boolean delete(Long id);

  DtoFormSubmissionExport exportCsv(Long formDefinitionId, LocalDate from, LocalDate to);

  DtoFormSubmissionCleanupResult cleanupOldSubmissions(Long formDefinitionId, int retentionDays);

  int cleanupAllExpiredSubmissions(int retentionDays);
}
