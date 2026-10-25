package com.cms.service.impl;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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

  private final HttpClient httpClient = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(5))
      .build();

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

    PushDeviceToken entity = pushDeviceTokenRepository.findByUserIdAndToken(userId, token)
        .orElseGet(() -> PushDeviceToken.builder()
            .userId(userId)
            .token(token)
            .platform(platform)
            .build());
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
      payload.put("voipTokens", devices.stream()
          .map(PushDeviceToken::getVoipToken)
          .filter(v -> v != null && !v.isBlank())
          .toList());
      dispatchWebhook(payload);
    } catch (Exception ex) {
      log.debug("Gelen arama VoIP push gonderimi atlandi: calleeUserId={}, hata={}",
          calleeUserId, ex.getMessage());
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
