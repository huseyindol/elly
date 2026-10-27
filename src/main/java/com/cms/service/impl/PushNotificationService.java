package com.cms.service.impl;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.DtoPushDeviceRequest;
import com.cms.dto.DtoPushDeviceResponse;
import com.cms.entity.PushDeviceToken;
import com.cms.exception.BadRequestException;
import com.cms.repository.PushDeviceTokenRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PushNotificationService {

  private static final Set<String> ALLOWED_PLATFORMS = Set.of("ANDROID", "IOS", "WEB");

  private final PushDeviceTokenRepository pushDeviceTokenRepository;
  private final ObjectMapper objectMapper;

  @Value("${app.push.webhook-url:${FCM_WEBHOOK_URL:}}")
  private String webhookUrl;

  @Value("${app.push.webhook-secret:${FCM_WEBHOOK_SECRET:}}")
  private String webhookSecret;

  @Value("${app.push.apns.p12-base64:${APNS_VOIP_P12_BASE64:}}")
  private String apnsVoipP12Base64;

  @Value("${app.push.apns.p12-path:${APNS_VOIP_P12_PATH:${user.home}/.elly-certs/apple-voip/elly_voip.p12}}")
  private String apnsVoipP12Path;

  @Value("${app.push.apns.p12-password:${APNS_VOIP_P12_PASSWORD:}}")
  private String apnsVoipP12Password;

  @Value("${app.push.apns.voip-topic:${APNS_VOIP_TOPIC:com.huseyindol.ellyMobileApp.voip}}")
  private String apnsVoipTopic;

  @Value("${app.push.apns.sandbox:${APNS_VOIP_SANDBOX:true}}")
  private boolean apnsVoipSandbox;

  private final HttpClient httpClient = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(5))
      .build();

  private volatile HttpClient apnsVoipClient;
  private volatile long apnsVoipClientLoadedMtime = -1L;

  @Transactional
  public DtoPushDeviceResponse registerDevice(Long userId, DtoPushDeviceRequest req) {
    if (req == null || req.getToken() == null || req.getToken().isBlank()) {
      throw new BadRequestException("Cihaz token zorunludur");
    }
    String token = req.getToken().trim();
    String platform = normalizePlatform(req.getPlatform());
    String voipToken = (req.getVoipToken() != null && !req.getVoipToken().isBlank())
        ? req.getVoipToken().trim()
        : null;
    String deviceName = (req.getDeviceName() != null && !req.getDeviceName().isBlank())
        ? req.getDeviceName().trim()
        : null;

    List<PushDeviceToken> existing = pushDeviceTokenRepository.findByUserId(userId).stream()
        .filter(d -> token.equals(d.getToken()))
        .toList();
    PushDeviceToken entity;
    if (!existing.isEmpty()) {
      entity = existing.get(0);
      if (existing.size() > 1) {
        pushDeviceTokenRepository.deleteAll(existing.subList(1, existing.size()));
      }
    } else {
      entity = PushDeviceToken.builder()
          .userId(userId)
          .token(token)
          .platform(platform)
          .build();
    }
    entity.setPlatform(platform);
    entity.setVoipToken(voipToken);
    entity.setDeviceName(deviceName);

    PushDeviceToken saved = pushDeviceTokenRepository.save(entity);
    return toDto(saved);
  }

  @Transactional
  public boolean unregisterDevice(Long userId, String token) {
    if (token == null || token.isBlank()) {
      return false;
    }
    return pushDeviceTokenRepository.deleteByUserIdAndToken(userId, token.trim()) > 0;
  }

  @Transactional(readOnly = true)
  public List<DtoPushDeviceResponse> listDevices(Long userId) {
    return pushDeviceTokenRepository.findByUserId(userId).stream()
        .map(this::toDto)
        .toList();
  }

  @Transactional(readOnly = true)
  public boolean hasRegisteredDevices(Long userId) {
    return !pushDeviceTokenRepository.findByUserId(userId).isEmpty();
  }

  public void sendPushToUser(
      Long userId,
      String title,
      String body,
      String link,
      Map<String, Object> metadata) {
    try {
      List<PushDeviceToken> devices = pushDeviceTokenRepository.findByUserId(userId);
      if (devices.isEmpty()) {
        return;
      }
      Map<String, Object> payload = new LinkedHashMap<>();
      payload.put("event", "NOTIFICATION");
      payload.put("userId", userId);
      payload.put("title", title);
      payload.put("body", body);
      payload.put("link", link);
      payload.put("metadata", metadata != null ? metadata : Map.of());
      payload.put("tokens", devices.stream().map(PushDeviceToken::getToken).toList());
      dispatchWebhook(payload);
    } catch (Exception ex) {
      log.debug("Push bildirim gonderimi atlandi: userId={}, hata={}", userId, ex.getMessage());
    }
  }

  public void sendIncomingCallWakeup(
      Long calleeUserId,
      String callId,
      Long callerUserId,
      String callerDisplayName,
      boolean ringAll,
      String tenantId) {
    try {
      List<PushDeviceToken> devices = pushDeviceTokenRepository.findByUserId(calleeUserId);
      if (devices.isEmpty()) {
        return;
      }
      List<String> voipTokens = devices.stream()
          .map(PushDeviceToken::getVoipToken)
          .filter(v -> v != null && !v.isBlank())
          .map(String::trim)
          .distinct()
          .toList();

      Map<String, Object> payload = new LinkedHashMap<>();
      payload.put("event", "INCOMING_CALL");
      payload.put("priority", "high");
      payload.put("calleeUserId", calleeUserId);
      payload.put("callId", callId);
      payload.put("callerUserId", callerUserId);
      payload.put("callerDisplayName", callerDisplayName);
      payload.put("ringAll", ringAll);
      payload.put("tenantId", tenantId);
      payload.put("tokens", devices.stream().map(PushDeviceToken::getToken).toList());
      payload.put("voipTokens", voipTokens);

      dispatchApnsVoipPush(voipTokens, callId, callerUserId, callerDisplayName, ringAll, tenantId);
      dispatchWebhook(payload);
    } catch (Exception ex) {
      log.debug("Gelen arama VoIP push gonderimi atlandi: calleeUserId={}, hata={}",
          calleeUserId, ex.getMessage());
    }
  }

  /**
   * Apple VoIP Services Certificate (.p12) mevcutsa dogrudan HTTP/2 uzerinden
   * APNs VoIP Push (PushKit -> CallKit) gonderir.
   */
  public void dispatchApnsVoipPush(
      List<String> voipTokens,
      String callId,
      Long callerUserId,
      String callerDisplayName,
      boolean ringAll,
      String tenantId) {
    if (voipTokens == null || voipTokens.isEmpty()) {
      return;
    }
    HttpClient client = resolveApnsVoipClient();
    if (client == null) {
      return;
    }
    String primaryHost = apnsVoipSandbox
        ? "https://api.development.push.apple.com"
        : "https://api.push.apple.com";
    String fallbackHost = apnsVoipSandbox
        ? "https://api.push.apple.com"
        : "https://api.development.push.apple.com";
    String topic = (apnsVoipTopic != null && !apnsVoipTopic.isBlank())
        ? apnsVoipTopic.trim()
        : "com.huseyindol.ellyMobileApp.voip";

    try {
      Map<String, Object> apnsBody = new LinkedHashMap<>();
      apnsBody.put("aps", Map.of("content-available", 1));
      apnsBody.put("callId", callId != null ? callId : UUID.randomUUID().toString());
      apnsBody.put("callerUserId", callerUserId);
      apnsBody.put("callerDisplayName", callerDisplayName != null ? callerDisplayName : "Elly Arama");
      apnsBody.put("ringAll", ringAll);
      apnsBody.put("tenantId", tenantId);
      String json = objectMapper.writeValueAsString(apnsBody);

      for (String voipToken : voipTokens) {
        String cleanToken = voipToken.trim().replaceAll("\\s+", "");
        if (cleanToken.isEmpty()) continue;
        sendApnsVoipRequest(client, primaryHost, fallbackHost, cleanToken, topic, json, callId);
      }
    } catch (Exception ex) {
      log.warn("APNs VoIP payload olusturulamadi: {}", ex.getMessage());
    }
  }

  private void sendApnsVoipRequest(
      HttpClient client,
      String host,
      String fallbackHost,
      String cleanToken,
      String topic,
      String json,
      String callId) {
    HttpRequest req = HttpRequest.newBuilder()
        .uri(URI.create(host + "/3/device/" + cleanToken))
        .version(HttpClient.Version.HTTP_2)
        .timeout(Duration.ofSeconds(5))
        .header("apns-push-type", "voip")
        .header("apns-priority", "10")
        .header("apns-expiration", "0")
        .header("apns-topic", topic)
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(json))
        .build();
    client.sendAsync(req, HttpResponse.BodyHandlers.ofString())
        .thenAccept(res -> {
          if (res.statusCode() == 200) {
            log.info("APNs VoIP push basarili: callId={}, host={}, topic={}", callId, host, topic);
          } else if (fallbackHost != null
              && res.statusCode() == 400
              && res.body() != null
              && (res.body().contains("BadDeviceToken") || res.body().contains("DeviceTokenNotForTopic"))) {
            log.info("APNs VoIP token ortami farkli ({}), yedek APNs sunucusu deneniyor: {}", host, fallbackHost);
            sendApnsVoipRequest(client, fallbackHost, null, cleanToken, topic, json, callId);
          } else {
            log.warn("APNs VoIP push yaniti: status={}, host={}, body={}", res.statusCode(), host, res.body());
          }
        })
        .exceptionally(err -> {
          log.warn("APNs VoIP push hatasi ({}): {}", host, err.getMessage());
          return null;
        });
  }

  private HttpClient resolveApnsVoipClient() {
    boolean hasBase64 = apnsVoipP12Base64 != null && !apnsVoipP12Base64.isBlank();
    if (!hasBase64 && (apnsVoipP12Path == null || apnsVoipP12Path.isBlank())) {
      return null;
    }
    try {
      Path path = (!hasBase64) ? Path.of(apnsVoipP12Path.trim()) : null;
      if (!hasBase64 && (path == null || !Files.isRegularFile(path))) {
        return null;
      }
      long mtime = hasBase64 ? apnsVoipP12Base64.hashCode() : Files.getLastModifiedTime(path).toMillis();
      if (apnsVoipClient != null && apnsVoipClientLoadedMtime == mtime) {
        return apnsVoipClient;
      }
      synchronized (this) {
        if (apnsVoipClient != null && apnsVoipClientLoadedMtime == mtime) {
          return apnsVoipClient;
        }
        String resolvedPass = apnsVoipP12Password;
        if ((resolvedPass == null || resolvedPass.isBlank()) && path != null) {
          Path passFile = path.getParent() != null ? path.getParent().resolve(".p12_pass") : null;
          if (passFile != null && Files.isRegularFile(passFile)) {
            resolvedPass = Files.readString(passFile).trim();
          }
        }
        char[] password = (resolvedPass != null) ? resolvedPass.toCharArray() : new char[0];
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream is = hasBase64
            ? new ByteArrayInputStream(Base64.getDecoder().decode(apnsVoipP12Base64.trim()))
            : new FileInputStream(path.toFile())) {
          keyStore.load(is, password);
        }
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, password);
        SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
        sslContext.init(kmf.getKeyManagers(), null, new SecureRandom());

        apnsVoipClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .sslContext(sslContext)
            .connectTimeout(Duration.ofSeconds(5))
            .build();
        apnsVoipClientLoadedMtime = mtime;
        log.info("Apple APNs VoIP PKCS12 sertifikasi yuklendi ({})", hasBase64 ? "APNS_VOIP_P12_BASE64" : path);
        return apnsVoipClient;
      }
    } catch (Exception ex) {
      log.warn("Apple APNs VoIP .p12 sertifikasi yuklenemedi: {}", ex.getMessage());
      return null;
    }
  }

  private void dispatchWebhook(Map<String, Object> payload) {
    if (webhookUrl == null || webhookUrl.isBlank()) {
      log.debug("Push webhook tanimli degil, push simule edildi: event={}, userId={}",
          payload.get("event"), payload.getOrDefault("userId", payload.get("calleeUserId")));
      return;
    }
    try {
      String json = objectMapper.writeValueAsString(payload);
      HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
          .uri(URI.create(webhookUrl.trim()))
          .timeout(Duration.ofSeconds(5))
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(json));
      if (webhookSecret != null && !webhookSecret.isBlank()) {
        reqBuilder.header("Authorization", "Bearer " + webhookSecret.trim());
      }
      httpClient.sendAsync(reqBuilder.build(), HttpResponse.BodyHandlers.discarding());
    } catch (Exception ex) {
      log.warn("Push webhook cagrisi basarisiz: {}", ex.getMessage());
    }
  }

  private String normalizePlatform(String raw) {
    if (raw == null || raw.isBlank()) {
      return "ANDROID";
    }
    String upper = raw.trim().toUpperCase(Locale.ROOT);
    return ALLOWED_PLATFORMS.contains(upper) ? upper : "ANDROID";
  }

  private DtoPushDeviceResponse toDto(PushDeviceToken entity) {
    return new DtoPushDeviceResponse(
        entity.getId(),
        entity.getUserId(),
        entity.getToken(),
        entity.getPlatform(),
        entity.getVoipToken(),
        entity.getDeviceName());
  }
}
