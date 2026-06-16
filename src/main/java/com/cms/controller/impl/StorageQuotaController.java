package com.cms.controller.impl;

import com.cms.controller.IStorageQuotaController;
import com.cms.dto.DtoStorageQuota;
import com.cms.dto.DtoStorageQuotaLimitRequest;
import com.cms.entity.RootEntityResponse;
import com.cms.service.IStorageQuotaService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Depolama kotası API'si — istek MEVCUT tenant context'inde çalışır.
 * Admin bir tenant'ın kotasını yönetmek için hedef tenant'ı URL path'inde taşır:
 * /api/v1/storage/tenant/{tid}/quota (JwtTenantFilter context'i URL'den set eder).
 */
@RestController
@RequestMapping("/api/v1/storage")
@RequiredArgsConstructor
public class StorageQuotaController extends BaseController implements IStorageQuotaController {

  private final IStorageQuotaService quotaService;

  @Override
  @GetMapping({"/quota", "/tenant/{tenantId}/quota"})
  @PreAuthorize("isAuthenticated()")
  public RootEntityResponse<DtoStorageQuota> getQuota() {
    return ok(quotaService.currentUsage());
  }

  @Override
  @PutMapping({"/quota/limit", "/tenant/{tenantId}/quota/limit"})
  @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
  public RootEntityResponse<DtoStorageQuota> setLimit(@RequestBody DtoStorageQuotaLimitRequest request) {
    return ok(quotaService.setLimit(request.getLimitBytes()));
  }

  @Override
  @PostMapping({"/quota/recompute", "/tenant/{tenantId}/quota/recompute"})
  @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
  public RootEntityResponse<DtoStorageQuota> recompute() {
    return ok(quotaService.recompute());
  }
}
