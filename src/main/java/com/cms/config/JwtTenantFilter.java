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
   * Tenant ID kaynak sırası (en yüksekten en düşüğe):
   * <ol>
   *   <li><b>{@code X-Tenant-Id} header</b> — admin'in panel UI'sında seçtiği tenant
   *       (örn. TC group'ları için). Header değeri "basedb" ya da boş ise null döner.</li>
   *   <li>Admin login + basedb-only path (auth, users, roles) → null (basedb).</li>
   *   <li>JWT {@code tenantId} claim'i — tenant user'ları için.</li>
   * </ol>
   *
   * <p><b>Chat ve {@code X-Tenant-Id}:</b> Chat (AC + TC) tek endpoint ailesi
   * (`/api/v1/chat/*`) altında. Admin AC'de iken header'ı göndermez → basedb'ye düşer.
   * Admin TC'de iken {@code X-Tenant-Id: tenant1} gönderir → tenant1 DB'sine düşer.
   * Tenant user (loginSource=tenant) JWT'inde zaten tenantId taşır.
   */
  private String resolveTenantId(HttpServletRequest request) {
    String path = request.getRequestURI();

    // 1) X-Tenant-Id header açıkça verilmişse, JWT claim'ini override eder.
    String headerTenant = request.getHeader("X-Tenant-Id");
    if (headerTenant != null && !headerTenant.isBlank()) {
      String normalized = headerTenant.trim();
      if ("basedb".equalsIgnoreCase(normalized) || "null".equalsIgnoreCase(normalized)) {
        log.debug("X-Tenant-Id header explicitly requests basedb for path: {}", path);
        return null;
      }
      log.debug("X-Tenant-Id header set TenantContext={} for path: {}", normalized, path);
      return normalized;
    }

    // 2) Chat REST: X-Tenant-Id yoksa AC (basedb). TC için header zorunlu.
    if (path.startsWith("/api/v1/chat/")) {
      log.debug("Chat path without X-Tenant-Id, forcing basedb: {}", path);
      return null;
    }

    // 3) Authorization yoksa null (anonim akış zaten public filter'a bırakılıyor)
    String authHeader = request.getHeader("Authorization");
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      return null;
    }

    try {
      String jwt = authHeader.substring(7);
      String tenantId = jwtUtil.extractTenantId(jwt);
      String loginSource = jwtUtil.extractLoginSource(jwt);

      boolean isBaseDbPath = path.startsWith("/api/v1/auth/")
          || path.startsWith("/api/v1/users")
          || path.startsWith("/api/v1/roles");

      // Admin login: auth, user ve role endpointleri her zaman basedb kullanır
      if ("admin".equals(loginSource) && isBaseDbPath) {
        log.debug("Admin login source, forcing basedb for path: {}", path);
        return null;
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
