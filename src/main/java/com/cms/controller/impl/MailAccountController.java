package com.cms.controller.impl;

import java.util.List;

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

import com.cms.controller.IMailAccountController;
import com.cms.dto.DtoMailAccountRequest;
import com.cms.dto.DtoMailAccountResponse;
import com.cms.dto.DtoMailTestRequest;
import com.cms.entity.MailAccount;
import com.cms.entity.RootEntityResponse;
import com.cms.service.IMailAccountService;
import com.cms.service.IMailTestService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/mail-accounts")
@RequiredArgsConstructor
public class MailAccountController extends BaseController implements IMailAccountController {

  private final IMailAccountService mailAccountService;
  private final IMailTestService mailTestService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Override
  @PreAuthorize("hasAuthority('mail:create')")
  public RootEntityResponse<DtoMailAccountResponse> create(
      @Valid @RequestBody DtoMailAccountRequest request) {
    return ok(mailAccountService.create(request));
  }

  @PutMapping("/{id}")
  @Override
  @PreAuthorize("hasAuthority('mail:update')")
  public RootEntityResponse<DtoMailAccountResponse> update(
      @PathVariable Long id,
      @Valid @RequestBody DtoMailAccountRequest request) {
    return ok(mailAccountService.update(id, request));
  }

  @GetMapping("/{id}")
  @Override
  @PreAuthorize("hasAuthority('mail:read')")
  public RootEntityResponse<DtoMailAccountResponse> getById(@PathVariable Long id) {
    return ok(mailAccountService.getById(id));
  }

  @GetMapping
  @Override
  @PreAuthorize("hasAuthority('mail:read')")
  public RootEntityResponse<List<DtoMailAccountResponse>> getAll() {
    return ok(mailAccountService.getAll());
  }

  @GetMapping("/active")
  @Override
  @PreAuthorize("hasAuthority('mail:read')")
  public RootEntityResponse<List<DtoMailAccountResponse>> getAllActive() {
    return ok(mailAccountService.getAllActive());
  }

  @DeleteMapping("/{id}")
  @Override
  @PreAuthorize("hasAuthority('mail:delete')")
  public RootEntityResponse<Boolean> delete(@PathVariable Long id) {
    return ok(mailAccountService.delete(id));
  }

  @PostMapping("/{id}/test")
  @Override
  @PreAuthorize("hasAuthority('mail:create')")
  public RootEntityResponse<String> testConnection(
      @PathVariable Long id,
      @Valid @RequestBody DtoMailTestRequest request) {
    MailAccount account = mailAccountService.getEntityById(id);
    mailTestService.sendTestEmail(account, request.getTestTo());
    return ok("Test maili basariyla gonderildi -> " + request.getTestTo());
  }

  @PostMapping("/{id}/verify")
  @Override
  @PreAuthorize("hasAuthority('mail:read')")
  public RootEntityResponse<String> verifyConnection(@PathVariable Long id) {
    mailAccountService.testConnection(id);
    return ok("SMTP baglantisi basarili");
  }
}
