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
 * Siteden (frontend) token olmadan gelen public istekleri yoneten filtre.
 *
 * URL pattern: /api/v1/public/{tenantId}/...
 *
 * Bu filtre:
 * 1. URL'den tenantId'yi cikarir ve TenantContext'e set eder
 * 2. Anonymous Authentication olusturur (sinirli CMS yetkileri)
 * 3. Request path'i rewrite eder: /api/v1/public/{tenantId}/... -> /api/v1/...
 * 4. Mevcut controller'lara yonlendirir
 *
 * Guvenlik:
 * - GET her path icin acik (read-only icerik)
 * - GET disindaki methodlar SADECE {@link #ALLOWED_WRITE_ENDPOINTS} listesindekiler
 *   icin izinlidir; eslesmeyen yazma istekleri 405 doner
 * - Sadece minimum CMS yetkileri verilir (admin endpointler 403 doner)
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

  /**
   * Public anonim istek icin verilen yetkiler.
   * Read yetkileri tum public GET'ler icin gereklidir.
   * forms:submit anonim site ziyaretcisinin form doldurabilmesi icin verilir.
   */
  private static final List<SimpleGrantedAuthority> PUBLIC_AUTHORITIES = List.of(
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
      new SimpleGrantedAuthority("basic_infos:read"),
      new SimpleGrantedAuthority("forms:submit"));

  /**
   * Public POST/PUT/DELETE icin acik olan endpoint'ler.
   * Path, /api/v1 prefix'i CIKARILMIS hali (yani filter'in remainingPath'i)
   * ile eslestirilir. Yeni bir public yazma endpoint'i acmadan once buraya
   * eklenmelidir; aksi halde filter 405 doner.
   */
  private static final List<PublicWriteEndpoint> ALLOWED_WRITE_ENDPOINTS = List.of(
      new PublicWriteEndpoint(HttpMethod.POST, Pattern.compile("^/forms/\\d+/submit$")),
      new PublicWriteEndpoint(HttpMethod.POST, Pattern.compile("^/auth/register$")),
      new PublicWriteEndpoint(HttpMethod.POST, Pattern.compile("^/auth/login$")));

  private final DataSourceConfig.TenantDataSourceProperties tenantDataSourceProperties;

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !request.getRequestURI().startsWith(PUBLIC_API_PREFIX);
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

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

    // Method/path izin kontrolu: GET her zaman acik, diger methodlar allowlist'te olmali
    if (!HttpMethod.GET.matches(request.getMethod()) && !isWriteAllowed(request.getMethod(), remainingPath)) {
      response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      response.setCharacterEncoding("UTF-8");
      response.getWriter().write(
          "{\"result\":false,\"status\":405,\"error\":\"Method Not Allowed\",\"message\":\""
              + request.getMethod() + " is not allowed on this public endpoint\"}");
      return;
    }

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
    log.debug("Public API: tenant={}, method={}, rewrite {} -> {}",
        tenantId, request.getMethod(), request.getRequestURI(), rewrittenPath);

    try {
      // TenantContext set et
      TenantContext.setTenantId(tenantId);

      // Diger filtrelerin bu istegi skip etmesi icin flag set et
      request.setAttribute(PUBLIC_API_ATTRIBUTE, true);

      // Anonymous authentication with limited public CMS authorities
      UsernamePasswordAuthenticationToken anonymousAuth = new UsernamePasswordAuthenticationToken(
          "public-" + tenantId, null, PUBLIC_AUTHORITIES);
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

  private boolean isWriteAllowed(String method, String remainingPath) {
    for (PublicWriteEndpoint endpoint : ALLOWED_WRITE_ENDPOINTS) {
      if (endpoint.method().matches(method) && endpoint.pathPattern().matcher(remainingPath).matches()) {
        return true;
      }
    }
    return false;
  }

  private record PublicWriteEndpoint(HttpMethod method, Pattern pathPattern) {
  }
}
