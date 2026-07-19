package com.cms.service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import com.cms.exception.BadRequestException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Google / GitHub authorization-code takası ve profil doğrulaması.
 *
 * <p>Frontend yalnız {@code code} gönderir; client secret'lar burada (backend'de) kullanılır.
 * Spring oauth2-client bağımlılığı YOKTUR — akış bilinçli olarak yalın tutuldu:
 * <ul>
 *   <li><b>Google:</b> code → token endpoint → dönen {@code id_token} payload'ı decode edilir.
 *       Token TLS ile doğrudan Google'dan alındığı için imza doğrulaması gerekmez; yine de
 *       {@code aud} (bizim client id), {@code iss} ve {@code email_verified} kontrol edilir.</li>
 *   <li><b>GitHub:</b> code → access_token → {@code /user} + {@code /user/emails}.
 *       Doğrulanmış (verified) primary e-posta ZORUNLU — yoksa anlaşılır hata.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OAuthProviderClient {

  /** Provider'dan doğrulanmış kullanıcı profili. */
  public record SocialProfile(String provider, String providerId, String email,
      String firstName, String lastName, String usernameHint) {
  }

  private final ObjectMapper objectMapper;
  private final RestClient restClient = RestClient.create();

  @Value("${app.oauth.google.client-id:}")
  private String googleClientId;
  @Value("${app.oauth.google.client-secret:}")
  private String googleClientSecret;

  @Value("${app.oauth.github.client-id:}")
  private String githubClientId;
  @Value("${app.oauth.github.client-secret:}")
  private String githubClientSecret;

  /** provider adına göre yönlendirir; desteklenmeyen provider → BadRequest. */
  public SocialProfile fetchProfile(String provider, String code, String redirectUri) {
    return switch (provider) {
      case "google" -> google(code, redirectUri);
      case "github" -> github(code, redirectUri);
      default -> throw new BadRequestException("Desteklenmeyen sosyal giriş sağlayıcısı: " + provider);
    };
  }

  // ─────────────────────────── Google ───────────────────────────

  private SocialProfile google(String code, String redirectUri) {
    requireConfigured(googleClientId, googleClientSecret, "Google");

    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("code", code);
    form.add("client_id", googleClientId);
    form.add("client_secret", googleClientSecret);
    form.add("redirect_uri", redirectUri);
    form.add("grant_type", "authorization_code");

    JsonNode tokenResponse = postForJson("https://oauth2.googleapis.com/token", form, null);
    String idToken = tokenResponse.path("id_token").asText(null);
    if (idToken == null) {
      log.warn("Google token yanıtında id_token yok: {}", tokenResponse.path("error").asText(""));
      throw new BadRequestException("Google girişi doğrulanamadı. Lütfen tekrar deneyin.");
    }

    JsonNode claims = decodeJwtPayload(idToken);
    // TLS ile doğrudan Google'dan geldi; yine de aud/iss/email_verified doğrula.
    if (!googleClientId.equals(claims.path("aud").asText(""))) {
      throw new BadRequestException("Google token doğrulaması başarısız (aud)");
    }
    String iss = claims.path("iss").asText("");
    if (!"https://accounts.google.com".equals(iss) && !"accounts.google.com".equals(iss)) {
      throw new BadRequestException("Google token doğrulaması başarısız (iss)");
    }
    if (!claims.path("email_verified").asBoolean(false)) {
      throw new BadRequestException("Google hesabınızın e-postası doğrulanmamış");
    }

    String email = claims.path("email").asText(null);
    if (email == null || email.isBlank()) {
      throw new BadRequestException("Google hesabınızdan e-posta alınamadı");
    }
    return new SocialProfile(
        "google",
        claims.path("sub").asText(),
        email.toLowerCase(),
        claims.path("given_name").asText(null),
        claims.path("family_name").asText(null),
        email.substring(0, email.indexOf('@')));
  }

  // ─────────────────────────── GitHub ───────────────────────────

  private SocialProfile github(String code, String redirectUri) {
    requireConfigured(githubClientId, githubClientSecret, "GitHub");

    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("code", code);
    form.add("client_id", githubClientId);
    form.add("client_secret", githubClientSecret);
    form.add("redirect_uri", redirectUri);

    JsonNode tokenResponse = postForJson("https://github.com/login/oauth/access_token", form, null);
    String accessToken = tokenResponse.path("access_token").asText(null);
    if (accessToken == null) {
      log.warn("GitHub token yanıtında access_token yok: {}", tokenResponse.path("error").asText(""));
      throw new BadRequestException("GitHub girişi doğrulanamadı. Lütfen tekrar deneyin.");
    }

    JsonNode userJson = getForJson("https://api.github.com/user", accessToken);
    String email = resolveVerifiedGithubEmail(userJson, accessToken);
    if (email == null) {
      // Kullanıcı kararı: doğrulanmış e-posta ZORUNLU — placeholder ile kayıt yok.
      throw new BadRequestException(
          "GitHub hesabınızdan doğrulanmış bir e-posta alınamadı. GitHub ayarlarından e-postanızı doğrulayın/görünür yapın.");
    }

    String login = userJson.path("login").asText("");
    String name = userJson.path("name").asText("");
    String firstName = null;
    String lastName = null;
    if (!name.isBlank()) {
      int sp = name.lastIndexOf(' ');
      firstName = sp > 0 ? name.substring(0, sp) : name;
      lastName = sp > 0 ? name.substring(sp + 1) : null;
    }
    return new SocialProfile(
        "github",
        userJson.path("id").asText(),
        email.toLowerCase(),
        firstName,
        lastName,
        login.isBlank() ? email.substring(0, email.indexOf('@')) : login);
  }

  /** Önce profil e-postası (public), yoksa /user/emails içinden primary+verified. */
  private String resolveVerifiedGithubEmail(JsonNode userJson, String accessToken) {
    try {
      JsonNode emails = getForJson("https://api.github.com/user/emails", accessToken);
      if (emails.isArray()) {
        // primary+verified öncelikli; yoksa herhangi bir verified
        String verified = null;
        for (JsonNode e : emails) {
          if (!e.path("verified").asBoolean(false)) {
            continue;
          }
          if (e.path("primary").asBoolean(false)) {
            return e.path("email").asText(null);
          }
          if (verified == null) {
            verified = e.path("email").asText(null);
          }
        }
        if (verified != null) {
          return verified;
        }
      }
    } catch (Exception e) {
      log.warn("GitHub /user/emails okunamadı: {}", e.getMessage());
    }
    String profileEmail = userJson.path("email").asText(null);
    return (profileEmail != null && !profileEmail.isBlank()) ? profileEmail : null;
  }

  // ─────────────────────────── Yardımcılar ───────────────────────────

  private void requireConfigured(String clientId, String clientSecret, String providerName) {
    if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
      throw new BadRequestException(providerName + " girişi bu sunucuda yapılandırılmamış");
    }
  }

  private JsonNode postForJson(String url, MultiValueMap<String, String> form, String bearer) {
    try {
      String body = restClient.post()
          .uri(url)
          .contentType(MediaType.APPLICATION_FORM_URLENCODED)
          .accept(MediaType.APPLICATION_JSON)
          .body(form)
          .retrieve()
          .body(String.class);
      return objectMapper.readTree(body == null ? "{}" : body);
    } catch (BadRequestException e) {
      throw e;
    } catch (Exception e) {
      log.warn("OAuth provider isteği başarısız ({}): {}", url, e.getMessage());
      throw new BadRequestException("Sosyal giriş sağlayıcısına ulaşılamadı. Lütfen tekrar deneyin.");
    }
  }

  private JsonNode getForJson(String url, String bearerToken) {
    try {
      String body = restClient.get()
          .uri(url)
          .header("Authorization", "Bearer " + bearerToken)
          .accept(MediaType.APPLICATION_JSON)
          .retrieve()
          .body(String.class);
      return objectMapper.readTree(body == null ? "{}" : body);
    } catch (Exception e) {
      log.warn("OAuth provider isteği başarısız ({}): {}", url, e.getMessage());
      throw new BadRequestException("Sosyal giriş sağlayıcısına ulaşılamadı. Lütfen tekrar deneyin.");
    }
  }

  /** JWT payload'ını (2. segment) imza doğrulamadan decode eder — kaynak TLS'li token endpoint'i. */
  private JsonNode decodeJwtPayload(String jwt) {
    try {
      String[] parts = jwt.split("\\.");
      if (parts.length < 2) {
        throw new IllegalArgumentException("JWT formatı geçersiz");
      }
      byte[] decoded = Base64.getUrlDecoder().decode(parts[1]);
      return objectMapper.readTree(new String(decoded, StandardCharsets.UTF_8));
    } catch (Exception e) {
      log.warn("id_token decode edilemedi: {}", e.getMessage());
      throw new BadRequestException("Google token doğrulaması başarısız");
    }
  }
}
