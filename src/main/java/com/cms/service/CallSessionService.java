package com.cms.service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import com.cms.config.TenantContext;
import com.cms.dto.DtoCallIce;
import com.cms.dto.DtoCallSdp;
import com.cms.dto.DtoCallSignal;
import com.cms.enums.CallSignalType;
import com.cms.enums.CallStatus;
import com.cms.service.CallLogService.PanelUserRef;
import com.cms.service.CallLogService.UserCallInfo;
import com.cms.util.ChatTopics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * WebRTC 1:1 çağrı orkestrasyonu — çekirdek durum makinesi ve sinyalleşme.
 *
 * <p>Registry in-memory {@link ConcurrentHashMap} (broker zaten in-memory, tek instance;
 * {@code compute} atomik claim sağlar). Yatay ölçeklemede Redis SETNX + broker relay gerekir (faz 2).
 *
 * <p><b>Sinyal teslimatı:</b> tenant'lı kullanıcı konusu
 * {@code /topic/tenant/{tenantId}/user/{userId}/rtc} (topic-tabanlı, bu projede güvenilir yol —
 * {@code ChatMembershipNotifier} deseni). Ek olarak Spring user-queue'ya da gönderilir
 * ({@code /user/queue/rtc}, tenant'lı kullanıcı anahtarıyla) — bazı ortamlarda user-destination teslim
 * etmediği için topic esas yoldur. Katılımcı denetimi tenant DAHİLDİR: kullanıcı numaraları tenant'a özeldir.
 * call_logs yazımları {@link CallLogService}'e; her çağrıdan önce TenantContext kurulur.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CallSessionService {

  private static final long RING_TIMEOUT_SECONDS = 30;

  private final SimpMessagingTemplate messagingTemplate;
  private final ChatPresenceService presenceService;
  private final CallLogService callLogService;
  private final TaskScheduler taskScheduler;

  @org.springframework.beans.factory.annotation.Autowired(required = false)
  private com.cms.service.impl.PushNotificationService pushNotificationService;

  private final Map<UUID, CallSession> calls = new ConcurrentHashMap<>();
  /** tenant'lı kullanıcı anahtarı → aktif/çalan callId (aynı anda tek çağrı). Anahtar tenant'lıdır: aynı
   *  numara başka tenant'ta başka kullanıcıdır, onun çağrısı bunu "meşgul" yapamaz. */
  private final Map<String, UUID> busy = new ConcurrentHashMap<>();
  /** tenant'lı kullanıcı anahtarı → client tarafı ref (tenant caller). Sinyaller ayrıca
   *  /topic/rtc-client/{ref}/rtc'ye gönderilir; tenant userId bilmeden kendi sinyallerine abone olur
   *  (prod'da userId zinciri kırılgan). Anahtar tenant'lıdır: başka tenant'taki aynı numaralı kullanıcının
   *  sinyalleri bu yansıtmaya düşmez. */
  private final Map<String, String> clientRefByUser = new ConcurrentHashMap<>();

  /** Kullanıcı eşlemelerinin anahtarı — tenant DAHİL (kullanıcı numaraları tenant veritabanına özeldir). */
  private static String userKey(String tenantId, Long userId) {
    return tenantId + ":" + userId;
  }

  /** Çağrı durumu (registry değeri). */
  static final class CallSession {
    UUID callId;
    String tenantId;
    Long callerUserId;
    String callerUsername;
    String callerDisplayName;
    Long calleeUserId;
    String calleeUsername;
    boolean ringAll;
    volatile CallStatus status;
    /** Hâlâ çalanlar: userId → username (panel-panel: [callee]; ring-all: tüm online panel kullanıcıları). */
    final Map<Long, String> ringing = new ConcurrentHashMap<>();
    ScheduledFuture<?> timeoutFuture;
  }

  // ─────────────────────────── Çağrı başlatma ───────────────────────────

  /** Panel-içi arama: {@code /app/rtc/call}. Hiyerarşi + presence + busy kontrolü.
   *  {@code groupId} verilir ve iki taraf da o grubun üyesiyse hiyerarşi atlanır. */
  public void startPanelCall(Long callerUserId, String callerUsername, String callerTenant,
      Long calleeUserId, String groupId) {
    if (calleeUserId == null || calleeUserId.equals(callerUserId)) {
      sendError(callerTenant, callerUserId, callerUsername, "Geçersiz arama hedefi");
      return;
    }
    if (callerTenant == null || callerTenant.isBlank()) {
      sendError(callerTenant, callerUserId, callerUsername, "Tenant context yok");
      return;
    }
    if (busy.containsKey(userKey(callerTenant, callerUserId))) {
      sendSignal(callerTenant, callerUserId, callerUsername, sig(CallSignalType.BUSY, null).reason("Zaten bir çağrıdasınız").build());
      return;
    }

    UserCallInfo caller;
    UserCallInfo callee;
    boolean sameGroup = false;
    try {
      TenantContext.setTenantId(callerTenant);
      caller = callLogService.lookupUser(callerUserId).orElse(null);
      callee = callLogService.lookupUser(calleeUserId).orElse(null);
      UUID gid = parseUuid(groupId);
      if (gid != null) {
        sameGroup = callLogService.sharedGroupMember(gid, callerUserId, calleeUserId);
      }
    } finally {
      TenantContext.clear();
    }
    if (caller == null || callee == null) {
      sendError(callerTenant, callerUserId, callerUsername, "Kullanıcı bulunamadı");
      return;
    }
    // Yön kuralı: panelden TENANT kullanıcısı aranamaz.
    if (callee.isTenant()) {
      sendError(callerTenant, callerUserId, callerUsername, "Tenant kullanıcısı panelden aranamaz");
      return;
    }
    // Hiyerarşi: kendi seviyesi ve altı (SUPER_ADMIN=4 herkesi). Aynı grubun üyeleriyse
    // hiyerarşi atlanır — o grupta herkes birbirini arayabilir (admin→superadmin dahil).
    if (!sameGroup && caller.roleLevel() < 4 && callee.roleLevel() > caller.roleLevel()) {
      sendError(callerTenant, callerUserId, callerUsername, "Bu kullanıcıyı arama yetkiniz yok");
      return;
    }
    if (!isUserReachable(calleeUserId, callerTenant)) {
      sendSignal(callerTenant, callerUserId, callerUsername, sig(CallSignalType.UNAVAILABLE, null).reason("Kullanıcı çevrimdışı").build());
      return;
    }
    if (busy.containsKey(userKey(callerTenant, calleeUserId))) {
      sendSignal(callerTenant, callerUserId, callerUsername, sig(CallSignalType.BUSY, null).reason("Kullanıcı meşgul").build());
      return;
    }

    CallSession s = new CallSession();
    s.callId = UUID.randomUUID();
    s.tenantId = callerTenant;
    s.callerUserId = callerUserId;
    s.callerUsername = callerUsername;
    s.callerDisplayName = callerUsername;
    s.calleeUserId = calleeUserId;
    s.calleeUsername = callee.username();
    s.ringAll = false;
    s.status = CallStatus.RINGING;
    s.ringing.put(calleeUserId, callee.username());
    calls.put(s.callId, s);
    busy.put(userKey(s.tenantId, callerUserId), s.callId);
    busy.put(userKey(s.tenantId, calleeUserId), s.callId);

    withTenant(s.tenantId, () -> callLogService.createRinging(s.callId, callerUserId, callerUsername));
    scheduleTimeout(s);

    // callee'ye gelen çağrı, caller'a "çalıyor" onayı
    sendSignal(s.tenantId, calleeUserId, callee.username(), sig(CallSignalType.INCOMING_CALL, s.callId)
        .peerUserId(callerUserId).peerDisplayName(callerUsername).ringAll(false).build());
    if (pushNotificationService != null) {
      withTenant(s.tenantId, () -> pushNotificationService.sendIncomingCallWakeup(
          calleeUserId, s.callId.toString(), callerUserId, callerUsername, false, s.tenantId));
    }
    sendSignal(s.tenantId, callerUserId, callerUsername, sig(CallSignalType.RINGING, s.callId)
        .peerUserId(calleeUserId).peerDisplayName(callee.username()).build());
  }

  /** Tenant destek araması (ring-all): {@code /app/tenant-rtc/{tenantId}/call}. */
  public void startTenantRingAll(Long callerUserId, String callerUsername, String tenantId, String clientRef) {
    if (tenantId == null || tenantId.isBlank()) {
      sendError(tenantId, callerUserId, callerUsername, "Tenant context yok");
      return;
    }
    // Caller sinyalleri (RINGING/ANSWERED/SDP/ICE/terminal) ayrıca clientRef topic'ine gitsin. Erken çıkan
    // yollarda (meşgul / temsilci yok) kayıt geri alınır: çağrı kurulmadıysa yansıtma kalmasın.
    String callerKey = userKey(tenantId, callerUserId);
    if (clientRef != null && !clientRef.isBlank()) {
      clientRefByUser.put(callerKey, clientRef);
    }
    if (busy.containsKey(callerKey)) {
      sendSignal(tenantId, callerUserId, callerUsername, sig(CallSignalType.BUSY, null).reason("Zaten bir çağrıdasınız").build());
      clientRefByUser.remove(callerKey);
      return;
    }

    List<PanelUserRef> panelUsers;
    try {
      TenantContext.setTenantId(tenantId);
      panelUsers = callLogService.findPanelUsers();
    } finally {
      TenantContext.clear();
    }
    List<PanelUserRef> online = panelUsers.stream()
        .filter(p -> !busy.containsKey(userKey(tenantId, p.userId())) && isUserReachable(p.userId(), tenantId))
        .toList();
    if (online.isEmpty()) {
      sendSignal(tenantId, callerUserId, callerUsername, sig(CallSignalType.UNAVAILABLE, null).reason("Şu an müsait temsilci yok").build());
      clientRefByUser.remove(callerKey);
      // Cevaplanamadan biten çağrıyı da kaydet (MISSED)
      UUID missedId = UUID.randomUUID();
      withTenant(tenantId, () -> {
        callLogService.createRinging(missedId, callerUserId, callerUsername);
        callLogService.markTerminal(missedId, CallStatus.MISSED);
      });
      return;
    }

    CallSession s = new CallSession();
    s.callId = UUID.randomUUID();
    s.tenantId = tenantId;
    s.callerUserId = callerUserId;
    s.callerUsername = callerUsername;
    s.callerDisplayName = callerUsername;
    s.ringAll = true;
    s.status = CallStatus.RINGING;
    online.forEach(p -> s.ringing.put(p.userId(), p.username()));
    calls.put(s.callId, s);
    busy.put(userKey(s.tenantId, callerUserId), s.callId);

    withTenant(s.tenantId, () -> callLogService.createRinging(s.callId, callerUserId, callerUsername));
    scheduleTimeout(s);

    online.forEach(p -> {
      sendSignal(s.tenantId, p.userId(), p.username(), sig(CallSignalType.INCOMING_CALL, s.callId)
          .peerUserId(callerUserId).peerDisplayName(callerUsername).ringAll(true).build());
      if (pushNotificationService != null) {
        withTenant(s.tenantId, () -> pushNotificationService.sendIncomingCallWakeup(
            p.userId(), s.callId.toString(), callerUserId, callerUsername, true, s.tenantId));
      }
    });
    sendSignal(s.tenantId, callerUserId, callerUsername, sig(CallSignalType.RINGING, s.callId).build());
  }

  // ─────────────────────────── Yaşam döngüsü ───────────────────────────

  /** Cevapla — atomic claim (ring-all'da ilk cevaplayan kazanır). */
  public void answer(UUID callId, Long answererUserId, String answererUsername, String tenantId) {
    boolean[] won = {false};
    CallSession s = calls.computeIfPresent(callId, (id, sess) -> {
      if (sess.status == CallStatus.RINGING && sess.ringing.containsKey(answererUserId)
            && sameTenant(sess, tenantId)) {
        sess.status = CallStatus.ANSWERED;
        sess.calleeUserId = answererUserId;
        sess.calleeUsername = answererUsername;
        won[0] = true;
      }
      return sess;
    });
    if (s == null || !won[0]) {
      // Çağrı yok veya başkası kaptı → cevaplayanın zili kapansın. Daima CEVAPLAYANIN kendi tenant'ına:
      // başka tenant'tan gelen bir cevapta s.tenantId kullanılsaydı, CALL_TAKEN karşı tenant'taki aynı
      // numaralı (asıl çalan) kullanıcıya gider ve onun zilini sustururdu. Meşru kaybedenler için ikisi aynı.
      sendSignal(tenantId, answererUserId, answererUsername,
          sig(CallSignalType.CALL_TAKEN, callId).build());
      return;
    }
    cancelTimeout(s);
    busy.put(userKey(s.tenantId, answererUserId), callId);

    // Ring-all: diğer çalanlara CALL_TAKEN
    s.ringing.forEach((uid, uname) -> {
      if (!uid.equals(answererUserId)) {
        sendSignal(s.tenantId, uid, uname, sig(CallSignalType.CALL_TAKEN, callId).build());
      }
    });
    s.ringing.clear();
    s.ringing.put(answererUserId, answererUsername);

    withTenant(s.tenantId, () -> callLogService.markAnswered(callId, answererUserId));

    // İki tarafa ANSWERED — offer'ı ARAYAN üretir
    sendSignal(s.tenantId, s.callerUserId, s.callerUsername, sig(CallSignalType.ANSWERED, callId)
        .peerUserId(answererUserId).peerDisplayName(answererUsername).build());
    sendSignal(s.tenantId, answererUserId, answererUsername, sig(CallSignalType.ANSWERED, callId)
        .peerUserId(s.callerUserId).peerDisplayName(s.callerDisplayName).build());
  }

  /** Reddet. */
  public void reject(UUID callId, Long userId, String tenantId) {
    CallSession s = calls.get(callId);
    if (s == null || s.status != CallStatus.RINGING || !sameTenant(s, tenantId)) return;

    if (s.ringAll) {
      s.ringing.remove(userId);
      if (s.ringing.isEmpty()) {
        terminate(s, CallStatus.REJECTED,
            () -> sendSignal(s.tenantId, s.callerUserId, s.callerUsername, sig(CallSignalType.REJECTED, callId).build()));
      }
    } else {
      // Panel-panel: yalnız callee reddedebilir
      if (!userId.equals(s.calleeUserId)) return;
      terminate(s, CallStatus.REJECTED,
          () -> sendSignal(s.tenantId, s.callerUserId, s.callerUsername, sig(CallSignalType.REJECTED, callId).build()));
    }
  }

  /** İptal (yalnız arayan, çalarken). */
  public void cancel(UUID callId, Long userId, String tenantId) {
    CallSession s = calls.get(callId);
    if (s == null || !sameTenant(s, tenantId)
        || !userId.equals(s.callerUserId) || s.status != CallStatus.RINGING) return;
    terminate(s, CallStatus.CANCELLED,
        () -> s.ringing.forEach((uid, uname) -> sendSignal(s.tenantId, uid, uname, sig(CallSignalType.CANCELLED, callId).build())));
  }

  /** Kapat (görüşme sırasında her iki taraf). Çalarken arayan kapatırsa iptal gibi. */
  public void hangup(UUID callId, Long userId, String username, String tenantId) {
    CallSession s = calls.get(callId);
    if (s == null || !sameTenant(s, tenantId)) return;
    boolean participant = userId.equals(s.callerUserId) || userId.equals(s.calleeUserId);
    if (!participant) return;

    if (s.status == CallStatus.RINGING) {
      if (userId.equals(s.callerUserId)) {
        cancel(callId, userId, tenantId);
      }
      return;
    }
    if (s.status == CallStatus.ANSWERED) {
      boolean callerHangup = userId.equals(s.callerUserId);
      Long otherUserId = callerHangup ? s.calleeUserId : s.callerUserId;
      String otherUsername = callerHangup ? s.calleeUsername : s.callerUsername;
      terminate(s, CallStatus.COMPLETED,
          () -> sendSignal(s.tenantId, otherUserId, otherUsername, sig(CallSignalType.HANGUP, callId).build()));
    }
  }

  /** SDP relay (opak) — yalnız çağrının iki tarafı, karşıya iletir. */
  public void relaySdp(UUID callId, Long fromUserId, String tenantId, DtoCallSdp sdp) {
    CallSession s = participantSession(callId, fromUserId, tenantId);
    if (s == null) return;
    boolean fromCaller = fromUserId.equals(s.callerUserId);
    Long targetUserId = fromCaller ? s.calleeUserId : s.callerUserId;
    String targetUsername = fromCaller ? s.calleeUsername : s.callerUsername;
    if (targetUserId == null) return;
    sendSignal(s.tenantId, targetUserId, targetUsername, sig(CallSignalType.SDP, callId).sdpType(sdp.getSdpType()).sdp(sdp.getSdp()).build());
  }

  /** ICE relay (opak). */
  public void relayIce(UUID callId, Long fromUserId, String tenantId, DtoCallIce ice) {
    CallSession s = participantSession(callId, fromUserId, tenantId);
    if (s == null) return;
    boolean fromCaller = fromUserId.equals(s.callerUserId);
    Long targetUserId = fromCaller ? s.calleeUserId : s.callerUserId;
    String targetUsername = fromCaller ? s.calleeUsername : s.callerUsername;
    if (targetUserId == null) return;
    sendSignal(s.tenantId, targetUserId, targetUsername, sig(CallSignalType.ICE, callId)
        .candidate(ice.getCandidate()).sdpMid(ice.getSdpMid()).sdpMLineIndex(ice.getSdpMLineIndex()).build());
  }

  // ─────────────────────────── Yardımcılar ───────────────────────────

  private boolean isUserReachable(Long userId, String tenantId) {
    if (presenceService.isOnline(tenantId, userId)) {
      return true;
    }
    if (pushNotificationService == null || tenantId == null || tenantId.isBlank()) {
      return false;
    }
    try {
      TenantContext.setTenantId(tenantId);
      return pushNotificationService.hasRegisteredDevices(userId);
    } catch (Exception e) {
      return false;
    } finally {
      TenantContext.clear();
    }
  }

  /** Çağrının katılımcısı mı — tenant DAHİL: kullanıcı numaraları tenant'a özeldir, aynı numara başka
   *  tenant'ta başkadır. */
  private CallSession participantSession(UUID callId, Long userId, String tenantId) {
    CallSession s = calls.get(callId);
    if (s == null || !sameTenant(s, tenantId)) return null;
    boolean participant = userId.equals(s.callerUserId) || userId.equals(s.calleeUserId);
    return participant ? s : null;
  }

  private boolean sameTenant(CallSession s, String tenantId) {
    return tenantId != null && tenantId.equals(s.tenantId);
  }

  private void scheduleTimeout(CallSession s) {
    s.timeoutFuture = taskScheduler.schedule(
        () -> onTimeout(s.callId),
        new Date(System.currentTimeMillis() + RING_TIMEOUT_SECONDS * 1000));
  }

  private void onTimeout(UUID callId) {
    CallSession s = calls.get(callId);
    if (s == null || s.status != CallStatus.RINGING) return;
    terminate(s, CallStatus.MISSED, () -> {
      s.ringing.forEach((uid, uname) -> sendSignal(s.tenantId, uid, uname, sig(CallSignalType.TIMEOUT, callId).build()));
      sendSignal(s.tenantId, s.callerUserId, s.callerUsername, sig(CallSignalType.TIMEOUT, callId).build());
    });
  }

  /** Durumu terminale çeker: log yaz + verilen sinyalleri gönder + registry/busy/timeout temizle. */
  private void terminate(CallSession s, CallStatus status, Runnable signals) {
    s.status = status;
    cancelTimeout(s);
    withTenant(s.tenantId, () -> callLogService.markTerminal(s.callId, status));
    try {
      signals.run();
    } finally {
      calls.remove(s.callId);
      busy.remove(userKey(s.tenantId, s.callerUserId), s.callId);
      clientRefByUser.remove(userKey(s.tenantId, s.callerUserId));
      if (s.calleeUserId != null) {
        busy.remove(userKey(s.tenantId, s.calleeUserId), s.callId);
      }
    }
  }

  private void cancelTimeout(CallSession s) {
    if (s.timeoutFuture != null) {
      s.timeoutFuture.cancel(false);
      s.timeoutFuture = null;
    }
  }

  private void withTenant(String tenantId, Runnable action) {
    try {
      TenantContext.setTenantId(tenantId);
      action.run();
    } catch (Exception e) {
      log.warn("Call log işlemi başarısız (tenant={}): {}", tenantId, e.getMessage());
    } finally {
      TenantContext.clear();
    }
  }

  /**
   * Sinyali hedefe iletir. Esas yol tenant'lı kullanıcı konusu ({@code /topic/tenant/{tid}/user/{uid}/rtc}) —
   * bu projede user-destination ({@code convertAndSendToUser}) her ortamda teslim etmediği için. Ek olarak
   * user-queue'ya da gönderilir (tenant'lı kullanıcı anahtarıyla).
   *
   * <p>GEÇİŞ: istemciler (admin, mobil, Mac ajanı) tenant'lı konuya geçene kadar sinyal eski tenant'sız
   * konuya da yazılır. Bütün istemciler yenilendiğinde bu satır ve eski konunun abonelik yetkisi kaldırılır.
   */
  private void sendSignal(String tenantId, Long userId, String username, DtoCallSignal signal) {
    messagingTemplate.convertAndSend(ChatTopics.userRtcTopic(tenantId, userId), signal);
    messagingTemplate.convertAndSend(ChatTopics.userRtcTopic(userId), signal);
    // Tenant caller: userId bilmeden kendi ref topic'inden alır.
    String clientRef = clientRefByUser.get(userKey(tenantId, userId));
    if (clientRef != null) {
      messagingTemplate.convertAndSend("/topic/rtc-client/" + clientRef + "/rtc", signal);
    }
    if (username != null) {
      messagingTemplate.convertAndSendToUser(ChatTopics.userKey(tenantId, username), "/queue/rtc", signal);
    }
  }

  private void sendError(String tenantId, Long userId, String username, String reason) {
    sendSignal(tenantId, userId, username, sig(CallSignalType.ERROR, null).reason(reason).build());
  }

  private DtoCallSignal.DtoCallSignalBuilder sig(CallSignalType type, UUID callId) {
    return DtoCallSignal.builder().type(type).callId(callId);
  }

  private static UUID parseUuid(String value) {
    if (value == null || value.isBlank()) return null;
    try {
      return UUID.fromString(value);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
}
