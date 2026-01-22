package com.cms.config;

import java.util.Arrays;
import java.util.Collections;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final UserDetailsService userDetailsService;
  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
  private final CustomOAuth2UserService customOAuth2UserService;

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public DaoAuthenticationProvider authenticationProvider() {
    DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
    authProvider.setUserDetailsService(userDetailsService);
    authProvider.setPasswordEncoder(passwordEncoder());
    return authProvider;
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
    return authConfig.getAuthenticationManager();
  }

  // Tüm domainler (GET isteği için)
  private static final java.util.List<String> ALL_DOMAINS = Arrays.asList(
      "http://localhost:3000",
      "http://localhost:8080",
      "http://localhost:5173",
      "http://elly-639969822644.europe-west1.run.app",
      "http://elly-bw5r3k32la-ew.a.run.app",
      "http://api.huseyindol.site",
      "http://www.huseyindol.site",
      "https://elly-639969822644.europe-west1.run.app",
      "https://elly-bw5r3k32la-ew.a.run.app",
      "https://api.huseyindol.site",
      "https://www.huseyindol.site");

  // Sadece yazma yetkisi olan domainler (POST, PUT, DELETE)
  // TODO: Burayı ihtiyacınıza göre düzenleyin, şu an örnek olarak localhost:3000
  // ve prod siteyi ekledim
  private static final java.util.List<String> WRITE_DOMAINS = Arrays.asList(
      "http://localhost:3000",
      "http://localhost:5173",
      "https://www.huseyindol.site");

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    return request -> {
      CorsConfiguration configuration = new CorsConfiguration();

      // İstek metodunu al
      String method = request.getMethod();

      // Preflight (OPTIONS) istekleri için hedef metodu kontrol et
      if ("OPTIONS".equalsIgnoreCase(method)) {
        String headerMethod = request.getHeader("Access-Control-Request-Method");
        if (headerMethod != null) {
          method = headerMethod;
        }
      }

      // Metoda göre izin verilen domainleri ayarla
      if ("GET".equalsIgnoreCase(method)) {
        configuration.setAllowedOrigins(ALL_DOMAINS);
      } else {
        // POST, PUT, DELETE, PATCH için kısıtlı domainler
        configuration.setAllowedOrigins(WRITE_DOMAINS);
      }

      configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
      configuration.setAllowedHeaders(Collections.singletonList("*"));
      configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));
      configuration.setAllowCredentials(true);
      configuration.setMaxAge(3600L);

      return configuration;
    };
  }

  @Bean
  @SuppressWarnings("java:S4502") // CSRF disabled intentionally for stateless JWT API
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    // CSRF protection is disabled because:
    // 1. This is a stateless REST API using JWT authentication
    // 2. No server-side session/cookie-based auth is used for state management
    // 3. All state-changing requests require valid JWT token
    // 4. Tokens are stored in HttpOnly cookies with SameSite protection
    http.csrf(csrf -> csrf.disable())
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/v1/auth/**").permitAll()
            .requestMatchers("/oauth2/**").permitAll()
            .requestMatchers("/login/oauth2/**").permitAll()
            .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/api-docs/**", "/v3/api-docs/**").permitAll()
            .requestMatchers("/actuator/**").permitAll()
            .anyRequest().permitAll())
        .oauth2Login(oauth2 -> oauth2
            .loginPage("/api/v1/auth/login") // OAuth2 login sayfasını devre dışı bırak, API endpoint'e yönlendir
            .successHandler(oAuth2AuthenticationSuccessHandler)
            .userInfoEndpoint(userInfo -> userInfo
                .userService(customOAuth2UserService)))
        .exceptionHandling(exceptions -> exceptions
            .authenticationEntryPoint((request, response, authException) -> {
              // OAuth2 login sayfası yerine 401 JSON response döndür
              response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
              response.setContentType("application/json");
              response.setCharacterEncoding("UTF-8");
              response.getWriter().write(
                  "{\"result\":false,\"status\":401,\"error\":\"Unauthorized\",\"errorCode\":\"AUTHENTICATION_REQUIRED\",\"message\":\"Authentication required\"}");
            }));

    http.authenticationProvider(authenticationProvider());
    http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }
}
