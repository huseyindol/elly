package com.cms.config;

import com.cms.exception.ForbiddenException;
import com.cms.exception.UnauthorizedException;
import com.cms.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Sadece loginSource="admin" (basedb) ile giriş yapmış kullanıcıların
 * erişebileceği endpoint'leri koruyan interceptor.
 */
@Component
@RequiredArgsConstructor
public class AdminLoginInterceptor implements HandlerInterceptor {

  private final JwtUtil jwtUtil;

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    String authHeader = request.getHeader("Authorization");

    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      throw new UnauthorizedException("Authentication required");
    }

    try {
      String jwt = authHeader.substring(7);
      String loginSource = jwtUtil.extractLoginSource(jwt);
      if (!"admin".equals(loginSource)) {
        throw new ForbiddenException("This endpoint requires admin panel login");
      }
    } catch (ForbiddenException | UnauthorizedException e) {
      throw e;
    } catch (Exception e) {
      throw new UnauthorizedException("Invalid or expired token");
    }

    return true;
  }
}
