package com.cms.config;

import com.cms.entity.RefreshToken;
import com.cms.entity.User;
import com.cms.repository.RefreshTokenRepository;
import com.cms.repository.UserRepository;
import com.cms.util.JwtUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

  private final UserRepository userRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final JwtUtil jwtUtil;

  @Value("${jwt.refresh.expiration:604800000}")
  private Long refreshTokenExpiration;

  @Value("${app.oauth2.redirect-uri:http://localhost:3000/oauth2/redirect}")
  private String redirectUri;

  @Override
  public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
      Authentication authentication) throws IOException, ServletException {

    OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
    String provider = determineProvider(authentication);

    // OAuth2 kullanıcı bilgilerini al
    Map<String, Object> attributes = oAuth2User.getAttributes();
    String providerId = getProviderId(attributes, provider);
    String email = getEmail(attributes, provider);
    String firstName = getFirstName(attributes, provider);
    String lastName = getLastName(attributes, provider);
    String username = getUsername(attributes, provider, email);

    // Kullanıcıyı bul veya oluştur
    User user = findOrCreateUser(provider, providerId, email, firstName, lastName, username);

    // Token version'ı artır (yeni token alındığında eski token'ları geçersiz kılmak
    // için)
    Long currentVersion = user.getTokenVersion() != null ? user.getTokenVersion() : 0L;
    user.setTokenVersion(currentVersion + 1);
    user = userRepository.save(user);

    // JWT token oluştur (güncel version ile)
    String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getTokenVersion());
    String refreshTokenString = jwtUtil.generateRefreshToken(user.getId(), user.getUsername());

    // Refresh token kaydet
    RefreshToken refreshToken = refreshTokenRepository.findByUserId(user.getId())
        .orElse(new RefreshToken());
    refreshToken.setToken(refreshTokenString);
    refreshToken.setUser(user);
    refreshToken.setIsRevoked(false);
    refreshToken.setExpiryDate(new Date(System.currentTimeMillis() + refreshTokenExpiration));
    refreshTokenRepository.save(refreshToken);

    // Redirect URL'e token'ları ekle
    String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
        .queryParam("token", token)
        .queryParam("refreshToken", refreshTokenString)
        .queryParam("userId", user.getId())
        .queryParam("username", user.getUsername())
        .queryParam("email", user.getEmail())
        .build().toUriString();

    getRedirectStrategy().sendRedirect(request, response, targetUrl);
  }

  private String determineProvider(Authentication authentication) {
    String authorizedClientRegistrationId = authentication.getName();
    if (authorizedClientRegistrationId.contains("google")) {
      return "google";
    } else if (authorizedClientRegistrationId.contains("facebook")) {
      return "facebook";
    } else if (authorizedClientRegistrationId.contains("github")) {
      return "github";
    } else if (authorizedClientRegistrationId.contains("twitter") || authorizedClientRegistrationId.contains("x")) {
      return "x";
    }
    return "unknown";
  }

  private String getProviderId(Map<String, Object> attributes, String provider) {
    switch (provider) {
      case "google":
        return (String) attributes.get("sub");
      case "facebook":
        return (String) attributes.get("id");
      case "github":
        return String.valueOf(attributes.get("id"));
      case "x":
        return (String) attributes.get("id_str");
      default:
        return null;
    }
  }

  private String getEmail(Map<String, Object> attributes, String provider) {
    switch (provider) {
      case "google":
        return (String) attributes.get("email");
      case "facebook":
        return (String) attributes.get("email");
      case "github":
        return (String) attributes.get("email");
      case "x":
        return null; // X/Twitter email vermez
      default:
        return null;
    }
  }

  private String getFirstName(Map<String, Object> attributes, String provider) {
    switch (provider) {
      case "google":
        return (String) attributes.get("given_name");
      case "facebook":
        return (String) attributes.get("first_name");
      case "github":
        String name = (String) attributes.get("name");
        return name != null && name.contains(" ") ? name.split(" ")[0] : name;
      case "x":
        return null;
      default:
        return null;
    }
  }

  private String getLastName(Map<String, Object> attributes, String provider) {
    switch (provider) {
      case "google":
        return (String) attributes.get("family_name");
      case "facebook":
        return (String) attributes.get("last_name");
      case "github":
        String name = (String) attributes.get("name");
        return name != null && name.contains(" ") ? name.substring(name.indexOf(" ") + 1) : null;
      case "x":
        return null;
      default:
        return null;
    }
  }

  private String getUsername(Map<String, Object> attributes, String provider, String email) {
    switch (provider) {
      case "google":
        return (String) attributes.getOrDefault("email", email);
      case "facebook":
        return (String) attributes.getOrDefault("email", email);
      case "github":
        return (String) attributes.getOrDefault("login", email);
      case "x":
        return (String) attributes.getOrDefault("screen_name", "x_user_" + attributes.get("id_str"));
      default:
        return email != null ? email : "user_" + System.currentTimeMillis();
    }
  }

  private User findOrCreateUser(String provider, String providerId, String email,
      String firstName, String lastName, String username) {
    // Önce provider ve providerId ile ara
    Optional<User> existingUser = userRepository.findByProviderAndProviderId(provider, providerId);

    if (existingUser.isPresent()) {
      return existingUser.get();
    }

    // Email ile ara (aynı email farklı provider'dan gelebilir)
    if (email != null) {
      Optional<User> userByEmail = userRepository.findByEmail(email);
      if (userByEmail.isPresent()) {
        // Mevcut kullanıcıya provider bilgisini ekle
        User user = userByEmail.get();
        user.setProvider(provider);
        user.setProviderId(providerId);
        return userRepository.save(user);
      }
    }

    // Yeni kullanıcı oluştur
    User newUser = new User();
    newUser.setProvider(provider);
    newUser.setProviderId(providerId);
    newUser.setEmail(email != null ? email : username + "@" + provider + ".com");
    newUser.setUsername(generateUniqueUsername(username, provider));
    newUser.setFirstName(firstName);
    newUser.setLastName(lastName);
    newUser.setPassword(null); // OAuth kullanıcıları için password yok
    newUser.setIsActive(true);

    return userRepository.save(newUser);
  }

  private String generateUniqueUsername(String baseUsername, String provider) {
    String username = baseUsername;
    int counter = 1;
    while (userRepository.existsByUsername(username)) {
      username = baseUsername + "_" + counter;
      counter++;
    }
    return username;
  }
}
