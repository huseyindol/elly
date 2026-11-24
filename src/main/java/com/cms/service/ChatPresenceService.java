package com.cms.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatPresenceService {

  private final RedisTemplate<String, Object> redisTemplate;

  private static final String KEY_PREFIX = "chat:presence:";

  @Value("${chat.presence.ttl-seconds:300}")
  private long presenceTtlSeconds;

  /** Bu instance'ın açık WS oturumu. */
  private record LiveSession(String tenantId, Long userId) {
  }

  /** Bu instance'ın açık WS oturumları: sessionId → oturum. [refreshLiveSessions] kayıtları bunlara göre tazeler. */
  private final ConcurrentHashMap<String, LiveSession> liveSessions = new ConcurrentHashMap<>();

  /**
   * Kayıt tenant'a göre ayrılır: kullanıcı numaraları her tenant veritabanında ayrı sayılır, başka tenant'taki
   * aynı numara başka kullanıcıdır. Tek kayıt kullanılsaydı o kullanıcı çevrimiçi görünürdü.
   */
  private static String key(String tenantId, Long userId) {
    return KEY_PREFIX + tenantId + ":" + userId;
  }

  public boolean userConnected(String tenantId, Long userId, String sessionId) {
    String key = key(tenantId, userId);
    Long count = redisTemplate.opsForValue().increment(key);
    redisTemplate.expire(key, presenceTtlSeconds, TimeUnit.SECONDS);
    if (sessionId != null) {
      // Sayaç yazıldıktan sonra: Redis hatasında bağlantı yine kurulur, yarım kalan oturum listeye girmez.
      liveSessions.put(sessionId, new LiveSession(tenantId, userId));
    }
    boolean firstSession = count != null && count == 1;
    if (firstSession) {
      log.debug("User {} is now ONLINE", userId);
    }
    return firstSession; // true = ilk bağlantı → ONLINE broadcast gerekir
  }

  public boolean userDisconnected(String tenantId, Long userId, String sessionId) {
    if (sessionId != null) {
      liveSessions.remove(sessionId);
    }
    String key = key(tenantId, userId);
    Long count = redisTemplate.opsForValue().decrement(key);
    boolean lastSession = count != null && count <= 0;
    if (lastSession) {
      redisTemplate.delete(key);
      log.debug("User {} is now OFFLINE", userId);
    }
    return lastSession; // true = son bağlantı kapandı → OFFLINE broadcast gerekir
  }

  public boolean isOnline(String tenantId, Long userId) {
    String key = key(tenantId, userId);
    Object val = redisTemplate.opsForValue().get(key);
    if (val == null) return false;
    try {
      return Long.parseLong(val.toString()) > 0;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  public void refreshHeartbeat(String tenantId, Long userId) {
    String key = key(tenantId, userId);
    if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
      redisTemplate.expire(key, presenceTtlSeconds, TimeUnit.SECONDS);
    }
  }

  /**
   * Açık oturumların çevrimiçi kaydını tazeler. Kaydın ömrü yalnızca bağlantı anında kuruluyordu ve onu
   * tazeleyen yoktu: 5 dakikadan uzun süren her bağlantı, WebSocket'i açık olsa da "çevrimdışı" görünüyordu.
   * Arama ulaşabilirliği ({@code CallSessionService}) bu kayda bakar — anlık bildirimi olmayan kullanıcı
   * 5 dakika sonra aranamaz hâle geliyordu.
   *
   * <p>Sayaç Redis'te ortak, oturum listesi instance'a özeldir: her instance yalnız kendi oturumlarını tazeler.
   * Kapanma olayı gelmeden kaybolan bir oturum (instance çökmesi, sessizce ölen soket) buradan silinmez; kaydı,
   * aynı kullanıcının başka açık oturumu kalmayınca ömrü dolunca kendini temizler. Oturum başına kesin sayım
   * (yatay ölçekleme ile birlikte) Faz 2 konusudur.
   */
  @Scheduled(fixedDelayString = "${chat.presence.refresh-ms:60000}", initialDelayString = "${chat.presence.refresh-ms:60000}")
  public void refreshLiveSessions() {
    Map<String, LiveSession> snapshot = new HashMap<>(liveSessions);
    int failed = 0;
    for (LiveSession session : new HashSet<>(snapshot.values())) {
      try {
        refreshHeartbeat(session.tenantId(), session.userId());
      } catch (RuntimeException e) {
        failed++;
      }
    }
    if (failed > 0) {
      log.warn("Presence refresh failed for {} of {} online users", failed, snapshot.size());
    }
  }
}
