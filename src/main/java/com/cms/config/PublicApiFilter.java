package com.cms.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Siteden (frontend) token olmadan gelen GET isteklerini yoneten filtre.
 *
 * URL pattern: GET /api/v1/public/{tenantId}/...
 *
 * Bu filtre:
 * 1. URL'den tenantId'yi cikarir ve TenantContext'e set eder
 * 2. Anonymous Authentication olusturur (sadece CMS read yetkileri)
 * 3. Request path'i rewrite eder: /api/v1/public/{tenantId}/... -> /api/v1/...
 * 4. Mevcut controller'lara yonlendirir
 *
 * Guvenlik:
 * - Sadece GET methoduna izin verir (POST/PUT/DELETE -> 405)
 * - Sadece CMS icerik okuma yetkileri verir (admin endpointler 403 doner)
 * - Gecersiz tenantId -> 400
 *
 * NOT: @Component kullanilmaz — cift filter kaydini onlemek icin
 * SecurityConfig'de bean olarak tanimlanir.
 */
@RequiredArgsConstructor
@Slf4j
public class PublicApiFilter extends OncePerRequestFilter {

  public static final String PUBLIC_API_ATTRIBUTE = "PUBLIC_API_REQUEST";

  private static final String PUBLIC_API_PREFIX = "/api/v1/public/";
  private static final Pattern TENANT_PATTERN = Pattern.compile("^/api/v1/public/([^/]+)(/.*)$");

  private static final List<SimpleGrantedAuthority> PUBLIC_READ_AUTHORITIES = List.of(
      new SimpleGrantedAuthority("contents:read"),
      new SimpleGrantedAuthority("posts:read"),
      new SimpleGrantedAuthority("pages:read"),
      new SimpleGrantedAuthority("banners:read"),
      new SimpleGrantedAuthority("components:read"),
      new SimpleGrantedAuthority("widgets:read"),
      new SimpleGrantedAuthority("assets:read"),
      new SimpleGrantedAuthority("comments:read"),
      new SimpleGrantedAuthority("ratings:read"),
      new SimpleGrantedAuthority("forms:read"),
      new SimpleGrantedAuthority("basic_infos:read"));

  private final DataSourceConfig.TenantDataSourceProperties tenantDataSourceProperties;

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !request.getRequestURI().startsWith(PUBLIC_API_PREFIX);
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    // Sadece GET'e izin ver
    if (!HttpMethod.GET.matches(request.getMethod())) {
      response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      response.setCharacterEncoding("UTF-8");
      response.getWriter().write(
          "{\"result\":false,\"status\":405,\"error\":\"Method Not Allowed\",\"message\":\"Public API only supports GET requests\"}");
      return;
    }

    // URL'den tenantId ve kalan path'i cikar
    Matcher matcher = TENANT_PATTERN.matcher(request.getRequestURI());
    if (!matcher.matches()) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      response.setCharacterEncoding("UTF-8");
      response.getWriter().write(
          "{\"result\":false,\"status\":400,\"error\":\"Bad Request\",\"message\":\"Invalid public API path. Expected: /api/v1/public/{tenantId}/...\"}");
      return;
    }

    String tenantId = matcher.group(1);
    String remainingPath = matcher.group(2);

    // Tenant validasyonu
    if (!tenantDataSourceProperties.getDatasources().containsKey(tenantId)) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      response.setCharacterEncoding("UTF-8");
      response.getWriter().write(
          "{\"result\":false,\"status\":400,\"error\":\"Bad Request\",\"message\":\"Unknown tenant: " + tenantId
              + "\"}");
      return;
    }

    String rewrittenPath = "/api/v1" + remainingPath;
    log.debug("Public API: tenant={}, rewrite {} -> {}", tenantId, request.getRequestURI(), rewrittenPath);

    try {
      // TenantContext set et
      TenantContext.setTenantId(tenantId);

      // Diger filtrelerin bu istegi skip etmesi icin flag set et
      request.setAttribute(PUBLIC_API_ATTRIBUTE, true);

      // Anonymous authentication with CMS read permissions
      UsernamePasswordAuthenticationToken anonymousAuth = new UsernamePasswordAuthenticationToken(
          "public-" + tenantId, null, PUBLIC_READ_AUTHORITIES);
      SecurityContextHolder.getContext().setAuthentication(anonymousAuth);

      // Path rewrite
      HttpServletRequest wrappedRequest = new HttpServletRequestWrapper(request) {
        @Override
        public String getRequestURI() {
          return rewrittenPath;
        }

        @Override
        public String getServletPath() {
          return rewrittenPath;
        }
      };

      filterChain.doFilter(wrappedRequest, response);
    } finally {
      TenantContext.clear();
      SecurityContextHolder.clearContext();
    }
  }
}
