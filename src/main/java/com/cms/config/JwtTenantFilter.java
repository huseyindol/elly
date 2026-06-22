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
 * Authorization Bearer JWT'sindeki {@code tenantId} claim'ini okuyup TenantContext'e
 * set eder. JwtAuthenticationFilter'dan ÖNCE çalışır; finally'de context'i temizler.
 *
 * <p><b>Tek kural:</b> Tenant DAİMA JWT {@code tenantId} claim'inden gelir. basedb /
 * default-tenant / admin-forcing YOKTUR. Public/anonim istekler (Authorization yok)
 * PublicApiFilter tarafından URL'deki {@code /api/v1/public/{tid}} ile ele alınır.
 * Authenticated bir istekte tenantId claim'i yoksa JwtAuthenticationFilter
 * {@code TENANT_REQUIRED} ile reddeder (sessiz fallback yok).
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

  /** Tenant = Authorization Bearer JWT'sindeki {@code tenantId} claim'i; yoksa null. */
  private String resolveTenantId(HttpServletRequest request) {
    String authHeader = request.getHeader("Authorization");
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      return null;
    }
    try {
      String tenantId = jwtUtil.extractTenantId(authHeader.substring(7));
      return (tenantId != null && !tenantId.isBlank()) ? tenantId : null;
    } catch (Exception e) {
      log.debug("Could not extract tenantId from JWT: {}", e.getMessage());
      return null;
    }
  }
}
