package com.cms.util;

import com.cms.entity.ChatGroup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Chat WebSocket topic isimlerinin üretimini tek noktada tutar.
 *
 * <p>AC=TC tek model: topic'ler daima tenant-aware —
 * {@code /topic/tenant/{tenantId}/group/{groupId}}. Legacy {@code /topic/group/{id}}
 * formatı yalnız tenant hiçbir şekilde çözülemezse üretilir (normalde olmamalı).
 */
public final class ChatTopics {

  private static final Logger log = LoggerFactory.getLogger(ChatTopics.class);

  private ChatTopics() {
    // utility
  }

  /**
   * AC=TC: entity'de tenant_id NULL kalmış eski (basedb dönemi) satırlar için mevcut
   * TenantContext'e düşülür — broadcast, panel'in abone olduğu tenant-aware topic'e
   * gitsin. (Broadcast eden servisler zaten grubun DB'sine context switch etmiş olur.)
   */
  private static String tenantOf(ChatGroup group) {
    String tid = group.getTenantId();
    return (tid == null || tid.isBlank()) ? com.cms.config.TenantContext.getTenantId() : tid;
  }

  /** Group entity'sinden mesaj topic'i türetir (tenant NULL fallback'li). */
  public static String messageTopic(ChatGroup group) {
    return messageTopic(tenantOf(group), group.getId());
  }

  /** Group entity'sinden ban topic'i türetir (tenant NULL fallback'li). */
  public static String banTopic(ChatGroup group) {
    return banTopic(tenantOf(group), group.getId());
  }

  public static String messageTopic(String tenantId, UUID groupId) {
    if (tenantId == null || tenantId.isBlank()) {
      return "/topic/group/" + groupId;
    }
    return "/topic/tenant/" + tenantId + "/group/" + groupId;
  }

  public static String typingTopic(ChatGroup group) {
    return messageTopic(group) + "/typing";
  }

  public static String readTopic(ChatGroup group) {
    return messageTopic(group) + "/read";
  }

  public static String typingTopic(String tenantId, UUID groupId) {
    return messageTopic(tenantId, groupId) + "/typing";
  }

  public static String readTopic(String tenantId, UUID groupId) {
    return messageTopic(tenantId, groupId) + "/read";
  }

  /** Ban/unban olayları (TC). Guest kendi sessionId'siyle eşleşince input'unu kilitler. */
  public static String banTopic(String tenantId, UUID groupId) {
    return messageTopic(tenantId, groupId) + "/bans";
  }

  // ───────────── Kullanıcıya özel konular (tenant'lı) ─────────────
  // Kullanıcı numaraları her tenant veritabanında ayrı sayılır: başka tenant'taki aynı numara başka
  // kullanıcıdır. Konu adı tenant'sız kalırsa o kullanıcı aynı konuya abone olup sinyalleri alabilir.

  private static String userTopic(String tenantId, Long userId, String suffix) {
    if (tenantId == null || tenantId.isBlank()) {
      // Sessiz kayıp olmasın: böyle bir konuya kimse abone değildir, yayın boşa gider.
      log.warn("Kullanıcı konusu tenant'sız üretildi; yayın teslim edilmeyecek: userId={}, konu={}", userId, suffix);
    }
    return "/topic/tenant/" + tenantId + "/user/" + userId + "/" + suffix;
  }

  /** WebRTC çağrı sinyalleri. */
  public static String userRtcTopic(String tenantId, Long userId) {
    return userTopic(tenantId, userId, "rtc");
  }

  /** Kullanıcı gruba davet edildiğinde kişisel topic (plain DtoChatGroup). */
  public static String userGroupJoinedTopic(String tenantId, Long userId) {
    return userTopic(tenantId, userId, "groups/joined");
  }

  /** Kullanıcı gruptan çıkarıldığında kişisel topic. */
  public static String userGroupRemovedTopic(String tenantId, Long userId) {
    return userTopic(tenantId, userId, "groups/removed");
  }

  /** JOINED / REMOVED membership event'leri (banner metni dahil). */
  public static String userMembershipTopic(String tenantId, Long userId) {
    return userTopic(tenantId, userId, "membership");
  }

  /**
   * Spring kullanıcı hedefli teslimat ({@code convertAndSendToUser}) oturumun kullanıcı ADINA bakar. Kullanıcı
   * adları da tenant'a özeldir (iki tenant'ta da "admin" olabilir); ad tenant'sız kalırsa bir tenant'ta üretilen
   * bildirim diğer tenant'taki aynı adlı oturuma da düşer. Yayıncılar bu anahtarı kullanır, WS oturumu da aynı
   * anahtarla kurulur (bkz. {@code ChatWebSocketSecurityConfig.authenticateConnect}). Tenant bilinmiyorsa hiçbir
   * oturumla eşleşmez: tenant'lar arası teslimat yerine teslimatsızlık.
   */
  public static String userKey(String tenantId, String username) {
    if (tenantId == null || tenantId.isBlank()) {
      log.warn("Kullanıcı hedefli teslimat anahtarı tenant'sız üretildi; hiçbir oturumla eşleşmez: username={}",
          username);
    }
    // Tenant, ayırıcıdan kaçırılır: "t1" + "a:b" ile "t1:a" + "b" aynı anahtara düşmesin.
    String tenant = tenantId == null ? "" : URLEncoder.encode(tenantId, StandardCharsets.UTF_8);
    return tenant + ":" + username;
  }

  // ───────────── GEÇİŞ: tenant'sız eski konumlar ─────────────
  // Yalnız istemciler tenant'lı konulara geçene kadar eski konuya da yayın yapmak için. Bütün istemciler
  // (admin, mobil, Mac ajanı) yenilendiğinde yayıncılar ve abonelik yetkisi kaldırılmalı.

  /** Tenant'sız eski konu (yalnız geçiş için). Yeni kod {@link #userGroupJoinedTopic(String, Long)}. */
  public static String userGroupJoinedTopic(Long userId) {
    return "/topic/user/" + userId + "/groups/joined";
  }

  /** Tenant'sız eski konu (yalnız geçiş için). Yeni kod {@link #userGroupRemovedTopic(String, Long)}. */
  public static String userGroupRemovedTopic(Long userId) {
    return "/topic/user/" + userId + "/groups/removed";
  }

  /** Tenant'sız eski konu (yalnız geçiş için). Yeni kod {@link #userMembershipTopic(String, Long)}. */
  public static String userMembershipTopic(Long userId) {
    return "/topic/user/" + userId + "/membership";
  }

  /** Tenant'sız eski konu (yalnız geçiş için). Yeni kod {@link #userRtcTopic(String, Long)}. */
  public static String userRtcTopic(Long userId) {
    return "/topic/user/" + userId + "/rtc";
  }
}
