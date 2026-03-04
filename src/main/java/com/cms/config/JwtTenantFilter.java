package com.cms.config;

import com.cms.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * X-Tenant-ID header'ından tenant JWT'sini okuyup TenantContext'e set eden
 * filtre.
 * 
 * Bu filtre JwtAuthenticationFilter'dan ÖNCE çalışır.
 * - X-Tenant-ID header'ından JWT token'ı alır
 * - JwtUtil ile decrypt ederek tenantId claim'ini çıkarır
 * - TenantContext'e set eder
 * - finally bloğunda TenantContext.clear() ile memory leak önler
 * 
 * Header yoksa veya geçersizse default tenant kullanılır.
 * 
 * Örnek kullanım:
 * GET /api/v1/pages/home
 * X-Tenant-ID: eyJhbGciOiJkaXIiLCJlbmMiOiJBMjU2R0NNIn0...
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(1)
public class JwtTenantFilter extends OncePerRequestFilter {

  private static final String TENANT_HEADER = "X-Tenant-ID";

  private final JwtUtil jwtUtil;

  @Value("${app.tenants.default-tenant:tenant1}")
  private String defaultTenant;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    try {
      String tenantId = resolveTenantId(request);
      TenantContext.setTenantId(tenantId);
      log.debug("Tenant resolved: {} for URI: {}", tenantId, request.getRequestURI());

      filterChain.doFilter(request, response);
    } finally {
      // Memory leak önleme - ThreadLocal her zaman temizlenmeli
      TenantContext.clear();
    }
  }

  /**
   * X-Tenant-ID header'ındaki JWT'den tenantId'yi çıkarır.
   * Header yoksa veya JWT geçersizse default tenant kullanılır.
   */
  private String resolveTenantId(HttpServletRequest request) {
    String tenantHeader = request.getHeader(TENANT_HEADER);

    if (tenantHeader == null || tenantHeader.isBlank()) {
      log.debug("No X-Tenant-ID header, using default tenant: {}", defaultTenant);
      return defaultTenant;
    }

    try {
      String tenantId = jwtUtil.extractTenantIdFromTenantToken(tenantHeader.trim());

      if (tenantId != null && !tenantId.isBlank()) {
        log.debug("Tenant from JWT: {}", tenantId);
        return tenantId;
      }

      log.debug("No tenantId in JWT, using default tenant: {}", defaultTenant);
      return defaultTenant;
    } catch (Exception e) {
      log.warn("Failed to decode tenant JWT: {}, using default tenant: {}", e.getMessage(), defaultTenant);
      return defaultTenant;
    }
  }
}
