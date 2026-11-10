package com.cms.config;

import java.io.IOException;

import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Yüklenen dosyaların anonim servis edildiği {@code /assets/**} yolu için ekstra
 * içerik-isolasyon header'ları (denetim T4):
 * <ul>
 *   <li>{@code Content-Security-Policy: sandbox} — tarayıcı, dönen içeriği (SVG, PDF vs.)
 *       script/form izinleri OLMAYAN korumalı belge gibi işler; same-origin script çalıştıramaz.</li>
 *   <li>{@code X-Content-Type-Options: nosniff} — MIME sniffing ile text/html'e dönüşümü engeller
 *       (defense-in-depth; Spring Security global header'ı da zaten set eder).</li>
 * </ul>
 * Not: {@code /assets/**} SecurityConfig'te GET permitAll'dir; upload tarafındaki uzantı
 * allowlist birinci savunma hattıdır, bu header'lar ikinci hattır.
 */
public class AssetsSecurityHeadersFilter extends OncePerRequestFilter {

  private static final String ASSETS_PREFIX = "/assets/";

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !request.getRequestURI().startsWith(ASSETS_PREFIX);
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    response.setHeader("Content-Security-Policy", "sandbox");
    response.setHeader("X-Content-Type-Options", "nosniff");
    filterChain.doFilter(request, response);
  }
}
