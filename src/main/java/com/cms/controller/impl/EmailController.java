package com.cms.controller.impl;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.cms.config.PermissionConstants;
import com.cms.controller.IEmailController;
import com.cms.dto.DtoEmailLog;
import com.cms.dto.EmailRequest;
import com.cms.entity.RootEntityResponse;
import com.cms.enums.EmailStatus;
import com.cms.service.IEmailService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/emails")
@RequiredArgsConstructor
public class EmailController implements IEmailController {

  private final IEmailService emailService;

  @PostMapping("/send")
  @ResponseStatus(HttpStatus.ACCEPTED)
  @Override
  @PreAuthorize("hasAuthority('" + PermissionConstants.EMAILS_SEND + "')")
  public RootEntityResponse<DtoEmailLog> sendEmail(@Valid @RequestBody EmailRequest request) {
    DtoEmailLog result = emailService.sendEmail(request);
    RootEntityResponse<DtoEmailLog> response = new RootEntityResponse<>();
    response.setResult(true);
    response.setMessage("Mail kuyruğa alındı, ID: " + result.getId());
    response.setData(result);
    return response;
  }

  @GetMapping("/templates")
  @Override
  @PreAuthorize("hasAuthority('" + PermissionConstants.EMAILS_READ + "')")
  public RootEntityResponse<List<String>> getAvailableTemplates() {
    return RootEntityResponse.ok(emailService.getAvailableTemplates());
  }

  /**
   * FAILED veya takilmis PENDING durumundaki bir EmailLog'u yeniden kuyruga koyar.
   * Status PENDING, retryCount 0, errorMessage null olarak sifirlanir.
   * SENT kayitlari retry edilmez — 400 doner.
   */
  @PostMapping("/{id}/retry")
  @ResponseStatus(HttpStatus.ACCEPTED)
  @Override
  @PreAuthorize("hasAuthority('" + PermissionConstants.EMAILS_RETRY + "')")
  public RootEntityResponse<DtoEmailLog> retry(@PathVariable("id") Long id) {
    DtoEmailLog result = emailService.retry(id);
    RootEntityResponse<DtoEmailLog> response = new RootEntityResponse<>();
    response.setResult(true);
    response.setMessage("Mail yeniden kuyruga alindi, ID: " + result.getId());
    response.setData(result);
    return response;
  }

  /**
   * Panel UI icin: tum email log'lari, opsiyonel status filtresi ile paginated.
   * Ornek: GET /api/v1/emails?status=FAILED&page=0&size=20&sort=id,desc
   */
  @GetMapping
  @Override
  @PreAuthorize("hasAuthority('" + PermissionConstants.EMAILS_READ + "')")
  public RootEntityResponse<Page<DtoEmailLog>> list(
      @RequestParam(value = "status", required = false) EmailStatus status,
      @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
    return RootEntityResponse.ok(emailService.list(status, pageable));
  }
}
