package com.cms.controller.impl;

import com.cms.config.DataSourceConfig;
import com.cms.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

  private final JwtUtil jwtUtil;
  private final DataSourceConfig.TenantDataSourceProperties tenantProperties;

  /**
   * Belirtilen tenant için JWT token üretir.
   * Bu token X-Tenant-ID header'ında kullanılır.
   *
   * Örnek: POST /api/v1/tenants/token?tenantId=tenant1
   * Dönen token'ı X-Tenant-ID header'ına ekleyerek diğer API'leri
   * çağırabilirsiniz.
   */
  @PostMapping("/token")
  @Operation(summary = "Tenant JWT Token Üret", description = "Belirtilen tenant ID için JWT token üretir. Bu token X-Tenant-ID header'ında kullanılır.")
  public ResponseEntity<Map<String, Object>> generateTenantToken(
      @RequestParam String tenantId) {

    // Tenant var mı kontrol et
    if (!tenantProperties.getDatasources().containsKey(tenantId)) {
      Map<String, Object> error = new HashMap<>();
      error.put("result", false);
      error.put("message", "Tenant not found: " + tenantId);
      error.put("availableTenants", tenantProperties.getDatasources().keySet());
      return ResponseEntity.badRequest().body(error);
    }

    String token = jwtUtil.generateTenantToken(tenantId);

    Map<String, Object> response = new HashMap<>();
    response.put("result", true);
    response.put("tenantId", tenantId);
    response.put("token", token);
    response.put("usage", "X-Tenant-ID: " + token);

    return ResponseEntity.ok(response);
  }

  /**
   * Mevcut tenant listesini döndürür.
   */
  @GetMapping("/list")
  @Operation(summary = "Tenant Listesi", description = "Konfigüre edilmiş tüm tenant'ları listeler.")
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
