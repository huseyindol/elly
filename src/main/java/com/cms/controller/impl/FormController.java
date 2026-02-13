package com.cms.controller.impl;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.controller.IFormController;
import com.cms.dto.DtoFormDefinition;
import com.cms.dto.DtoFormDefinitionIU;
import com.cms.dto.DtoFormSubmission;
import com.cms.dto.DtoFormSubmit;
import com.cms.dto.PagedResponse;
import com.cms.entity.FormDefinition;
import com.cms.entity.FormSubmission;
import com.cms.entity.RootEntityResponse;
import com.cms.mapper.FormMapper;
import com.cms.service.IFormDefinitionService;
import com.cms.service.IFormSubmissionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/forms")
@RequiredArgsConstructor
public class FormController extends BaseController implements IFormController {

  private final IFormDefinitionService formDefinitionService;
  private final IFormSubmissionService formSubmissionService;
  private final FormMapper formMapper;

  // ==================== FormDefinition Endpoints ====================

  @Override
  @PostMapping
  public RootEntityResponse<DtoFormDefinition> createFormDefinition(@RequestBody DtoFormDefinitionIU dto) {
    FormDefinition entity = formMapper.toEntity(dto);
    FormDefinition saved = formDefinitionService.save(entity);
    return ok(formMapper.toDto(saved));
  }

  @Override
  @PutMapping("/{id}")
  public RootEntityResponse<DtoFormDefinition> updateFormDefinition(
      @PathVariable Long id,
      @RequestBody DtoFormDefinitionIU dto) {
    FormDefinition existing = formDefinitionService.getById(id);
    formMapper.updateFromDto(dto, existing);
    FormDefinition saved = formDefinitionService.save(existing);
    return ok(formMapper.toDto(saved));
  }

  @Override
  @GetMapping("/{id}")
  public RootEntityResponse<DtoFormDefinition> getFormDefinitionById(@PathVariable Long id) {
    FormDefinition entity = formDefinitionService.getById(id);
    return ok(formMapper.toDto(entity));
  }

  @Override
  @GetMapping("/list")
  public RootEntityResponse<List<DtoFormDefinition>> getAllFormDefinitions() {
    List<FormDefinition> entities = formDefinitionService.getAll();
    return ok(formMapper.toDtoList(entities));
  }

  @Override
  @GetMapping("/list/active")
  public RootEntityResponse<List<DtoFormDefinition>> getActiveFormDefinitions() {
    List<FormDefinition> entities = formDefinitionService.getAllActive();
    return ok(formMapper.toDtoList(entities));
  }

  @Override
  @GetMapping("/list/paged")
  public RootEntityResponse<PagedResponse<DtoFormDefinition>> getAllFormDefinitionsPaged(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "id,asc") String sort) {
    Pageable pageable = createPageable(page, size, sort);
    Page<FormDefinition> pageResult = formDefinitionService.getAllPaged(pageable);
    List<DtoFormDefinition> dtos = formMapper.toDtoList(pageResult.getContent());
    return ok(PagedResponse.from(pageResult, dtos));
  }

  @Override
  @DeleteMapping("/{id}")
  public RootEntityResponse<Boolean> deleteFormDefinition(@PathVariable Long id) {
    Boolean deleted = formDefinitionService.delete(id);
    if (deleted) {
      return ok(deleted);
    }
    return error("FormDefinition not deleted");
  }

  // ==================== FormSubmission Endpoints ====================

  @Override
  @PostMapping("/{formId}/submit")
  public RootEntityResponse<DtoFormSubmission> submitForm(
      @PathVariable Long formId,
      @RequestBody DtoFormSubmit dto) {
    FormSubmission submission = formSubmissionService.submitForm(formId, dto.getPayload());
    return ok(formMapper.toSubmissionDto(submission));
  }

  @Override
  @GetMapping("/{formId}/submissions")
  public RootEntityResponse<List<DtoFormSubmission>> getSubmissionsByFormId(@PathVariable Long formId) {
    List<FormSubmission> submissions = formSubmissionService.getByFormDefinitionId(formId);
    return ok(formMapper.toSubmissionDtoList(submissions));
  }

  @Override
  @GetMapping("/{formId}/submissions/paged")
  public RootEntityResponse<PagedResponse<DtoFormSubmission>> getSubmissionsByFormIdPaged(
      @PathVariable Long formId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "submittedAt,desc") String sort) {
    Pageable pageable = createPageable(page, size, sort);
    Page<FormSubmission> pageResult = formSubmissionService.getByFormDefinitionIdPaged(formId, pageable);
    List<DtoFormSubmission> dtos = formMapper.toSubmissionDtoList(pageResult.getContent());
    return ok(PagedResponse.from(pageResult, dtos));
  }

  @Override
  @GetMapping("/submissions/{submissionId}")
  public RootEntityResponse<DtoFormSubmission> getSubmissionById(@PathVariable Long submissionId) {
    FormSubmission submission = formSubmissionService.getById(submissionId);
    return ok(formMapper.toSubmissionDto(submission));
  }

  @Override
  @GetMapping("/{formId}/submissions/count")
  public RootEntityResponse<Long> getSubmissionCount(@PathVariable Long formId) {
    Long count = formSubmissionService.countByFormId(formId);
    return ok(count);
  }
}
