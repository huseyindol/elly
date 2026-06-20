package com.cms.config;

import com.cms.util.AuthTokenResolver;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
  private final DataSourceConfig.TenantDataSourceProperties tenantProperties;

  /**
   * URL-tenant kuralı: admin'in hedef-tenant işlemleri için tenant URL path'inde taşınır —
   * {@code /api/v1/chat/tenant/{tid}/**} ve {@code /api/v1/storage/tenant/{tid}/**}.
   * WS destination'ları ({@code /app/tenant-chat/{tid}/..}) ve public REST
   * ({@code /api/v1/public/{tid}/..}) ile aynı kalıp: kimlik JWT'de, hedef tenant URL'de.
   */
  private static final Pattern URL_TENANT =
      Pattern.compile("^/api/v1/(?:chat|storage)/tenant/([a-zA-Z0-9_-]+)(?:/|$)");

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return Boolean.TRUE.equals(request.getAttribute(PublicApiFilter.PUBLIC_API_ATTRIBUTE));
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    try {
      String tenantId;
      String urlTenant = matchUrlTenant(request.getRequestURI());
      if (urlTenant != null && "admin".equals(safeLoginSource(request))) {
        // URL-tenant yalnız admin kimliğiyle: tenant user başka tenant'a sıçrayamaz.
        if (!tenantProperties.getDatasources().containsKey(urlTenant)) {
          response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
          response.setContentType("application/json;charset=UTF-8");
          response.getWriter().write(
              "{\"result\":false,\"status\":400,\"error\":\"Bad Request\",\"message\":\"Unknown tenant: "
                  + urlTenant + "\"}");
          return;
        }
        tenantId = urlTenant;
      } else {
        tenantId = resolveTenantId(request);
      }
      if (tenantId != null) {
        TenantContext.setTenantId(tenantId);
      }
      log.debug("Tenant resolved: {} for URI: {}", tenantId, request.getRequestURI());

      filterChain.doFilter(request, response);
    } finally {
      TenantContext.clear();
    }
  }

  /** Path {@code /api/v1/{chat|storage}/tenant/{tid}/**} kalıbına uyuyorsa tid, değilse null. */
  private String matchUrlTenant(String path) {
    Matcher m = URL_TENANT.matcher(path);
    return m.find() ? m.group(1) : null;
  }

  /** Token'dan (Bearer header veya accessToken cookie) loginSource — yok/bozuksa null. */
  private String safeLoginSource(HttpServletRequest request) {
    String jwt = AuthTokenResolver.resolve(request);
    if (jwt == null) {
      return null;
    }
    try {
      return jwtUtil.extractLoginSource(jwt);
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Tenant ID kaynak sırası:
   * <ol>
   *   <li>URL-tenant (doFilterInternal'da): {@code /api/v1/{chat|storage}/tenant/{tid}/**}
   *       — yalnız admin kimliğiyle; admin'in hedef-tenant işlemleri (TC, kota).</li>
   *   <li>Chat + Notifications path'leri → her zaman basedb (null).</li>
   *   <li>Admin login + basedb-only path (auth, users, roles) → null (basedb).</li>
   *   <li>JWT {@code tenantId} claim'i — tenant user'larının kendi tenancy'si.</li>
   * </ol>
   *
   * <p><b>Kural:</b> kimlik her zaman JWT'de, hedef tenant her zaman URL'de.
   * {@code X-Tenant-Id} header ve tenant-switch token desteklenmez.</p>
   */
  private String resolveTenantId(HttpServletRequest request) {
    String path = request.getRequestURI();

    // 1) Chat + Notifications REST: her zaman basedb.
    if (path.startsWith("/api/v1/chat/") || path.startsWith("/api/v1/notifications")) {
      log.debug("Basedb-only path, forcing basedb: {}", path);
      return null;
    }

    // 2) Token yoksa (Bearer header de accessToken cookie de yok) null —
    //    anonim akış zaten public filter'a bırakılıyor.
    String jwt = AuthTokenResolver.resolve(request);
    if (jwt == null) {
      return null;
    }

    try {
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
