package com.cms.controller;

import java.util.List;

import com.cms.dto.DtoFormDefinition;
import com.cms.dto.DtoFormDefinitionIU;
import com.cms.dto.DtoFormSubmission;
import com.cms.dto.DtoFormSubmit;
import com.cms.dto.PagedResponse;
import com.cms.entity.RootEntityResponse;

public interface IFormController {

  // FormDefinition endpoints
  RootEntityResponse<DtoFormDefinition> createFormDefinition(DtoFormDefinitionIU dto);

  RootEntityResponse<DtoFormDefinition> updateFormDefinition(Long id, DtoFormDefinitionIU dto);

  RootEntityResponse<DtoFormDefinition> getFormDefinitionById(Long id);

  RootEntityResponse<List<DtoFormDefinition>> getAllFormDefinitions();

  RootEntityResponse<List<DtoFormDefinition>> getActiveFormDefinitions();

  RootEntityResponse<PagedResponse<DtoFormDefinition>> getAllFormDefinitionsPaged(int page, int size, String sort);

  RootEntityResponse<Boolean> deleteFormDefinition(Long id);

  // FormSubmission endpoints
  RootEntityResponse<DtoFormSubmission> submitForm(Long formId, DtoFormSubmit dto);

  RootEntityResponse<List<DtoFormSubmission>> getSubmissionsByFormId(Long formId);

  RootEntityResponse<PagedResponse<DtoFormSubmission>> getSubmissionsByFormIdPaged(Long formId, int page, int size,
      String sort);

  RootEntityResponse<DtoFormSubmission> getSubmissionById(Long submissionId);

  RootEntityResponse<Long> getSubmissionCount(Long formId);
}
