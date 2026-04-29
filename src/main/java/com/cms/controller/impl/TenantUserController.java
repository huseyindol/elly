package com.cms.controller.impl;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.cms.controller.ITenantUserController;
import com.cms.dto.DtoAdminUserCreate;
import com.cms.dto.DtoAdminUserUpdate;
import com.cms.dto.DtoUserResponse;
import com.cms.entity.RootEntityResponse;
import com.cms.service.ITenantUserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Panel adminin tenant DB'lerindeki kullanıcıları yönetmesi için endpoint'ler.
 * Tüm endpoint'ler AdminLoginInterceptor (loginSource=admin) ile korunur.
 */
@RestController
@RequestMapping("/api/v1/admin/tenants/{tenantId}/users")
@RequiredArgsConstructor
public class TenantUserController extends BaseController implements ITenantUserController {

  private final ITenantUserService tenantUserService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Override
  @PreAuthorize("hasAuthority('users:manage')")
  public RootEntityResponse<DtoUserResponse> createUser(
      @PathVariable String tenantId,
      @Valid @RequestBody DtoAdminUserCreate dto) {
    return ok(tenantUserService.createUser(tenantId, dto));
  }

  @GetMapping
  @Override
  @PreAuthorize("hasAuthority('users:manage')")
  public RootEntityResponse<List<DtoUserResponse>> getAllUsers(@PathVariable String tenantId) {
    return ok(tenantUserService.getAllUsers(tenantId));
  }

  @GetMapping("/{id}")
  @Override
  @PreAuthorize("hasAuthority('users:manage')")
  public RootEntityResponse<DtoUserResponse> getUserById(
      @PathVariable String tenantId,
      @PathVariable Long id) {
    return ok(tenantUserService.getUserById(tenantId, id));
  }

  @PutMapping("/{id}")
  @Override
  @PreAuthorize("hasAuthority('users:manage')")
  public RootEntityResponse<DtoUserResponse> updateUser(
      @PathVariable String tenantId,
      @PathVariable Long id,
      @Valid @RequestBody DtoAdminUserUpdate dto) {
    return ok(tenantUserService.updateUser(tenantId, id, dto));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Override
  @PreAuthorize("hasAuthority('users:manage')")
  public RootEntityResponse<Void> deleteUser(
      @PathVariable String tenantId,
      @PathVariable Long id) {
    tenantUserService.deleteUser(tenantId, id);
    return ok(null);
  }

  @PatchMapping("/{id}/status")
  @Override
  @PreAuthorize("hasAuthority('users:manage')")
  public RootEntityResponse<DtoUserResponse> setUserStatus(
      @PathVariable String tenantId,
      @PathVariable Long id,
      @RequestParam boolean isActive) {
    return ok(tenantUserService.setUserStatus(tenantId, id, isActive));
  }
}
