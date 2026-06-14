package com.cms.controller.impl;

import com.cms.config.DataSourceConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.access.prepost.PreAuthorize;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Tenant yönetimi endpoint'leri.
 * Tenant JWT token üretimi ve mevcut tenant listesi.
 */
@RestController
@RequestMapping("/api/v1/tenants")
@RequiredArgsConstructor
@Tag(name = "Tenant Management", description = "Tenant JWT token üretimi ve yönetimi")
public class TenantController {

  private final DataSourceConfig.TenantDataSourceProperties tenantProperties;

  /**
   * Mevcut tenant listesini döndürür.
   */
  @GetMapping("/list")
  @Operation(summary = "Tenant Listesi", description = "Konfigüre edilmiş tüm tenant'ları listeler.")
  @PreAuthorize("hasAuthority('tenants:read')")
  public ResponseEntity<Map<String, Object>> listTenants() {
    Set<String> tenantIds = tenantProperties.getDatasources().keySet();
    String defaultTenant = tenantProperties.getDefaultTenant();

    Map<String, Object> response = new HashMap<>();
    response.put("result", true);
    response.put("defaultTenant", defaultTenant);
    response.put("tenants", tenantIds);
    response.put("totalCount", tenantIds.size());

    return ResponseEntity.ok(response);
  }
}
