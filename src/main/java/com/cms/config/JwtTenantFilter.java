package com.cms.config;

import com.cms.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Authorization header'ındaki main JWT'den tenantId claim'ini okuyup
 * TenantContext'e set eden filtre.
 *
 * Bu filtre JwtAuthenticationFilter'dan ÖNCE çalışır.
 * - Authorization Bearer token'ını alır
 * - JwtUtil ile decrypt ederek tenantId claim'ini çıkarır
 * - TenantContext'e set eder
 * - finally bloğunda TenantContext.clear() ile memory leak önler
 *
 * Token yoksa veya tenantId claim'i null ise TenantContext boş kalır,
 * routing datasource default tenant'ı (basedb) kullanır.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(1)
public class JwtTenantFilter extends OncePerRequestFilter {

  private final JwtUtil jwtUtil;

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return Boolean.TRUE.equals(request.getAttribute(PublicApiFilter.PUBLIC_API_ATTRIBUTE));
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    try {
      String tenantId = resolveTenantId(request);
      if (tenantId != null) {
        TenantContext.setTenantId(tenantId);
      }
      log.debug("Tenant resolved: {} for URI: {}", tenantId, request.getRequestURI());

      filterChain.doFilter(request, response);
    } finally {
      TenantContext.clear();
    }
  }

  /**
   * Authorization Bearer token'ından tenantId claim'ini çıkarır.
   *
   * Routing kuralları:
   * - loginSource="admin" → her zaman basedb (null)
   * - /api/v1/chat/ + loginSource="admin" → basedb (admin chat izolasyonu)
   * - /api/v1/chat/ + loginSource="website"|"tenant" → JWT'deki tenantId (null ise basedb)
   * - Diğer path'ler + loginSource="tenant"/"website" → JWT'deki tenantId
   */
  private String resolveTenantId(HttpServletRequest request) {
    String authHeader = request.getHeader("Authorization");

    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      return null;
    }

    try {
      String jwt = authHeader.substring(7);
      String tenantId = jwtUtil.extractTenantId(jwt);
      String loginSource = jwtUtil.extractLoginSource(jwt);
      String path = request.getRequestURI();

      // Admin her zaman basedb'ye gider
      if ("admin".equals(loginSource)) {
        log.debug("Admin loginSource, forcing basedb for path: {}", path);
        return null;
      }

      // Chat path — tenant/guest kendi DB'sine gider
      if (path.startsWith("/api/v1/chat/") || path.equals("/api/v1/chat")) {
        log.debug("Chat path ({}), tenantId={}", loginSource, tenantId);
        return (tenantId != null && !tenantId.isBlank()) ? tenantId : null;
      }

      if (tenantId != null && !tenantId.isBlank()) {
        log.debug("Tenant from JWT: {} for path: {}", tenantId, path);
        return tenantId;
      }
      return null;
    } catch (Exception e) {
      log.debug("Could not extract tenantId from JWT: {}", e.getMessage());
      return null;
    }
  }
}
