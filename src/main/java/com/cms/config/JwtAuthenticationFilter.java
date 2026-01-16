package com.cms.config;

import com.cms.entity.User;
import com.cms.repository.UserRepository;
import com.cms.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtUtil jwtUtil;
  private final UserDetailsService userDetailsService;
  private final UserRepository userRepository;
  private final ObjectMapper objectMapper;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {

    final String authorizationHeader = request.getHeader("Authorization");

    // Authorization header yoksa token işlemlerine girmeyi atla
    if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
      chain.doFilter(request, response);
      return;
    }

    // Authorization header varsa token işlemlerine gir
    // Authorization header gönderilmişse token MUTLAKA geçerli olmalı
    String jwt = authorizationHeader.substring(7);
    final String username;

    try {
      username = jwtUtil.extractUsername(jwt);
    } catch (Exception e) {
      // Token geçersiz veya decrypt edilemiyor
      // Authorization header gönderilmişse token geçerli olmalı, hata döndür
      sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "BAD_CREDENTIALS", "Invalid or expired token");
      return;
    }

    // Username çıkarıldıysa ve henüz authentication yapılmadıysa
    if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
      try {
        // User'ı veritabanından bul (tokenVersion için)
        final String finalUsername = username; // Lambda için final
        User user = userRepository.findByUsername(finalUsername)
            .orElseThrow(() -> new org.springframework.security.core.userdetails.UsernameNotFoundException(
                "User not found: " + finalUsername));

        UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

        // Token'ı validate et (tokenVersion kontrolü ile)
        Long currentTokenVersion = user.getTokenVersion() != null ? user.getTokenVersion() : 0L;
        if (!jwtUtil.validateToken(jwt, userDetails.getUsername(), currentTokenVersion)) {
          // Token geçersiz veya eski version (yeni token alındığında eski token geçersiz
          // olur)
          sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "BAD_CREDENTIALS", "Invalid or expired token");
          return;
        }

        // Token geçerli, authentication set et
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
            userDetails, null, userDetails.getAuthorities());
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);
      } catch (org.springframework.security.core.userdetails.UsernameNotFoundException e) {
        // User bulunamadı
        sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "BAD_CREDENTIALS", "Invalid token or user not found");
        return;
      } catch (Exception e) {
        // Başka bir hata
        sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "BAD_CREDENTIALS", "Invalid token or user not found");
        return;
      }
    }

    chain.doFilter(request, response);
  }

  private void sendErrorResponse(HttpServletResponse response, HttpStatus status, String errorCode, String message)
      throws IOException {
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");

    Map<String, Object> errorResponse = new HashMap<>();
    errorResponse.put("result", false);
    errorResponse.put("status", status.value());
    errorResponse.put("error", status.getReasonPhrase());
    errorResponse.put("errorCode", errorCode);
    errorResponse.put("message", message);

    objectMapper.writeValue(response.getWriter(), errorResponse);
  }
}
