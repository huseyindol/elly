package com.cms.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor to track request timing for all REST API endpoints.
 * Logs request duration, URI, method, and status code.
 */
@Component
public class RequestTimingInterceptor implements HandlerInterceptor {

  private static final Logger logger = LoggerFactory.getLogger(RequestTimingInterceptor.class);
  private static final String START_TIME = "startTime";
  private static final long SLOW_REQUEST_THRESHOLD_MS = 1000; // 1 second

  @Override
  public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
      @NonNull Object handler) {
    // Only track API requests (skip static resources, actuator, swagger, etc.)
    String uri = request.getRequestURI();
    if (shouldTrackRequest(uri)) {
      long startTime = System.currentTimeMillis();
      request.setAttribute(START_TIME, startTime);

      // Add request context to MDC for logging correlation
      String requestId = java.util.UUID.randomUUID().toString().substring(0, 8);
      MDC.put("requestId", requestId);
      MDC.put("uri", uri);
      MDC.put("method", request.getMethod());
      MDC.put("remoteAddr", getClientIpAddress(request));

      logger.debug("Request started - {} {} [{}]", request.getMethod(), uri, requestId);
    }
    return true;
  }

  @Override
  public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
      @NonNull Object handler, @Nullable Exception ex) {
    String uri = request.getRequestURI();
    if (shouldTrackRequest(uri)) {
      Long startTime = (Long) request.getAttribute(START_TIME);
      if (startTime != null) {
        long duration = System.currentTimeMillis() - startTime;
        int status = response.getStatus();

        // Log request completion
        if (ex != null) {
          logger.error(
              "Request failed - {} {} Status: {} Duration: {}ms Exception: {} [{}]",
              request.getMethod(), uri, status, duration, ex.getClass().getSimpleName(),
              MDC.get("requestId"));
        } else if (duration > SLOW_REQUEST_THRESHOLD_MS) {
          logger.warn(
              "⚠️ SLOW REQUEST - {} {} Status: {} Duration: {}ms [{}]",
              request.getMethod(), uri, status, duration, MDC.get("requestId"));
        } else {
          logger.info(
              "Request completed - {} {} Status: {} Duration: {}ms [{}]",
              request.getMethod(), uri, status, duration, MDC.get("requestId"));
        }

        // Clear MDC
        MDC.clear();
      }
    }
  }

  /**
   * Determines if the request should be tracked.
   * Tracks all /api/** requests, excludes static resources, actuator, swagger, etc.
   */
  private boolean shouldTrackRequest(String uri) {
    // Track all API endpoints
    if (uri.startsWith("/api/")) {
      return true;
    }
    // Track OAuth2 endpoints
    if (uri.startsWith("/oauth2/") || uri.startsWith("/login/oauth2/")) {
      return true;
    }
    // Skip static resources, actuator, swagger, etc.
    return false;
  }

  /**
   * Gets the client IP address from the request.
   */
  private String getClientIpAddress(HttpServletRequest request) {
    String xForwardedFor = request.getHeader("X-Forwarded-For");
    if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
      return xForwardedFor.split(",")[0].trim();
    }
    String xRealIp = request.getHeader("X-Real-IP");
    if (xRealIp != null && !xRealIp.isEmpty()) {
      return xRealIp;
    }
    return request.getRemoteAddr();
  }
}

