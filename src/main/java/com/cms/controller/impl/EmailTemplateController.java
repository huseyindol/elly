package com.cms.controller.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.cms.config.PermissionConstants;
import com.cms.controller.IEmailTemplateController;
import com.cms.dto.DtoEmailTemplate;
import com.cms.dto.DtoEmailTemplateIU;
import com.cms.dto.DtoEmailTemplatePreviewRequest;
import com.cms.dto.DtoEmailTemplatePreviewResponse;
import com.cms.entity.RootEntityResponse;
import com.cms.service.IEmailTemplateService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/email-templates")
@RequiredArgsConstructor
public class EmailTemplateController implements IEmailTemplateController {

  private final IEmailTemplateService emailTemplateService;

  @GetMapping
  @Override
  @PreAuthorize("hasAuthority('" + PermissionConstants.EMAIL_TEMPLATES_READ + "')")
  public RootEntityResponse<Page<DtoEmailTemplate>> list(
      @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
    return RootEntityResponse.ok(emailTemplateService.list(pageable));
  }

  @GetMapping("/{key}")
  @Override
  @PreAuthorize("hasAuthority('" + PermissionConstants.EMAIL_TEMPLATES_READ + "')")
  public RootEntityResponse<DtoEmailTemplate> getByKey(@PathVariable("key") String key) {
    return RootEntityResponse.ok(emailTemplateService.getByKey(key));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Override
  @PreAuthorize("hasAuthority('" + PermissionConstants.EMAIL_TEMPLATES_MANAGE + "')")
  public RootEntityResponse<DtoEmailTemplate> create(
      @Valid @RequestBody DtoEmailTemplateIU request) {
    return RootEntityResponse.ok(emailTemplateService.create(request));
  }

  @PutMapping("/{key}")
  @Override
  @PreAuthorize("hasAuthority('" + PermissionConstants.EMAIL_TEMPLATES_MANAGE + "')")
  public RootEntityResponse<DtoEmailTemplate> update(
      @PathVariable("key") String key,
      @Valid @RequestBody DtoEmailTemplateIU request) {
    return RootEntityResponse.ok(emailTemplateService.update(key, request));
  }

  @DeleteMapping("/{key}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Override
  @PreAuthorize("hasAuthority('" + PermissionConstants.EMAIL_TEMPLATES_MANAGE + "')")
  public RootEntityResponse<Void> delete(@PathVariable("key") String key) {
    emailTemplateService.delete(key);
    RootEntityResponse<Void> response = new RootEntityResponse<>();
    response.setResult(true);
    response.setMessage("Template '" + key + "' silindi");
    return response;
  }

  @PostMapping("/{key}/preview")
  @Override
  @PreAuthorize("hasAuthority('" + PermissionConstants.EMAIL_TEMPLATES_READ + "')")
  public RootEntityResponse<DtoEmailTemplatePreviewResponse> preview(
      @PathVariable("key") String key,
      @RequestBody DtoEmailTemplatePreviewRequest request) {
    return RootEntityResponse.ok(
        emailTemplateService.preview(key, request.getData()));
  }
}
