package com.cms.filter;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ApiKeyFilter extends OncePerRequestFilter {

  @Value("${email.api.key:dev-api-key-change-me}")
  private String apiKey;

  private static final String API_KEY_HEADER = "X-API-KEY";
  private static final String EMAIL_API_PATH = "/api/v1/emails";

  @Override
  protected void doFilterInternal(HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    String requestPath = request.getRequestURI();

    // Only apply to email API endpoints
    if (!requestPath.startsWith(EMAIL_API_PATH)) {
      filterChain.doFilter(request, response);
      return;
    }

    String requestApiKey = request.getHeader(API_KEY_HEADER);

    if (requestApiKey == null || !requestApiKey.equals(apiKey)) {
      log.warn("Unauthorized access attempt to email API from IP: {}",
          request.getRemoteAddr());
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      response.setContentType("application/json");
      response.setCharacterEncoding("UTF-8");
      response.getWriter().write(
          "{\"result\":false,\"status\":401,\"error\":\"Unauthorized\",\"errorCode\":\"INVALID_API_KEY\",\"message\":\"Invalid or missing X-API-KEY header\"}");
      return;
    }

    filterChain.doFilter(request, response);
  }
}
