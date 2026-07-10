package com.cms.service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * WebRTC 1:1 çağrı orkestrasyonu — çekirdek durum makinesi ve sinyalleşme.
 *
 * <p>Registry in-memory {@link ConcurrentHashMap} (broker zaten in-memory, tek instance;
 * {@code compute} atomik claim sağlar). Yatay ölçeklemede Redis SETNX + broker relay gerekir (faz 2).
 *
 * <p>Sinyaller {@code convertAndSendToUser(username, "/queue/rtc", dto)} ile gider (user-destination
 * anahtarı USERNAME). call_logs yazımları {@link CallLogService}'e; her çağrıdan önce TenantContext kurulur.
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

  private final Map<UUID, CallSession> calls = new ConcurrentHashMap<>();
  /** userId → aktif/çalan callId (aynı anda tek çağrı). */
  private final Map<Long, UUID> busy = new ConcurrentHashMap<>();

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
    /** Hâlâ çalan username'ler (panel-panel: [callee]; ring-all: tüm online panel kullanıcıları). */
    final Set<String> ringing = new CopyOnWriteArraySet<>();
    ScheduledFuture<?> timeoutFuture;
  }

  // ─────────────────────────── Çağrı başlatma ───────────────────────────

  /** Panel-içi arama: {@code /app/rtc/call}. Hiyerarşi + presence + busy kontrolü. */
  public void startPanelCall(Long callerUserId, String callerUsername, String callerTenant, Long calleeUserId) {
    log.info("[rtc] startPanelCall: caller={}({}) tenant={} callee={}",
        callerUsername, callerUserId, callerTenant, calleeUserId);
    if (calleeUserId == null || calleeUserId.equals(callerUserId)) {
      sendError(callerUsername, "Geçersiz arama hedefi");
      return;
    }
    if (callerTenant == null || callerTenant.isBlank()) {
      sendError(callerUsername, "Tenant context yok");
      return;
    }
    if (busy.containsKey(callerUserId)) {
      sendSignal(callerUsername, sig(CallSignalType.BUSY, null).reason("Zaten bir çağrıdasınız").build());
      return;
    }

    UserCallInfo caller;
    UserCallInfo callee;
    try {
      TenantContext.setTenantId(callerTenant);
      caller = callLogService.lookupUser(callerUserId).orElse(null);
      callee = callLogService.lookupUser(calleeUserId).orElse(null);
    } finally {
      TenantContext.clear();
    }
    if (caller == null || callee == null) {
      sendError(callerUsername, "Kullanıcı bulunamadı");
      return;
    }
    // Yön kuralı: panelden TENANT kullanıcısı aranamaz.
    if (callee.isTenant()) {
      sendError(callerUsername, "Tenant kullanıcısı panelden aranamaz");
      return;
    }
    // Hiyerarşi: kendi seviyesi ve altı (SUPER_ADMIN=4 herkesi).
    if (caller.roleLevel() < 4 && callee.roleLevel() > caller.roleLevel()) {
      sendError(callerUsername, "Bu kullanıcıyı arama yetkiniz yok");
      return;
    }
    boolean calleeOnline = presenceService.isOnline(calleeUserId);
    log.info("[rtc] presence check: callee={} online={}", calleeUserId, calleeOnline);
    if (!calleeOnline) {
      sendSignal(callerUsername, sig(CallSignalType.UNAVAILABLE, null).reason("Kullanıcı çevrimdışı").build());
      return;
    }
    if (busy.containsKey(calleeUserId)) {
      sendSignal(callerUsername, sig(CallSignalType.BUSY, null).reason("Kullanıcı meşgul").build());
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
    s.ringing.add(callee.username());
    calls.put(s.callId, s);
    busy.put(callerUserId, s.callId);
    busy.put(calleeUserId, s.callId);

    withTenant(s.tenantId, () -> callLogService.createRinging(s.callId, callerUserId, callerUsername));
    scheduleTimeout(s);

    // callee'ye gelen çağrı, caller'a "çalıyor" onayı
    sendSignal(callee.username(), sig(CallSignalType.INCOMING_CALL, s.callId)
        .peerUserId(callerUserId).peerDisplayName(callerUsername).ringAll(false).build());
    sendSignal(callerUsername, sig(CallSignalType.RINGING, s.callId)
        .peerUserId(calleeUserId).peerDisplayName(callee.username()).build());
  }

  /** Tenant destek araması (ring-all): {@code /app/tenant-rtc/{tenantId}/call}. */
  public void startTenantRingAll(Long callerUserId, String callerUsername, String tenantId) {
    log.info("[rtc] startTenantRingAll: caller={}({}) tenant={}", callerUsername, callerUserId, tenantId);
    if (tenantId == null || tenantId.isBlank()) {
      sendError(callerUsername, "Tenant context yok");
      return;
    }
    if (busy.containsKey(callerUserId)) {
      sendSignal(callerUsername, sig(CallSignalType.BUSY, null).reason("Zaten bir çağrıdasınız").build());
      return;
    }

    List<PanelUserRef> panelUsers;
    try {
      TenantContext.setTenantId(tenantId);
      panelUsers = callLogService.findPanelUsers();
    } finally {
      TenantContext.clear();
    }
    log.info("[rtc] ring-all panel users in {}: {}", tenantId,
        panelUsers.stream().map(p -> p.username() + "(" + p.userId() + ",online=" + presenceService.isOnline(p.userId()) + ")").toList());
    List<PanelUserRef> online = panelUsers.stream()
        .filter(p -> presenceService.isOnline(p.userId()) && !busy.containsKey(p.userId()))
        .toList();
    log.info("[rtc] ring-all online targets: {}", online.stream().map(PanelUserRef::username).toList());
    if (online.isEmpty()) {
      sendSignal(callerUsername, sig(CallSignalType.UNAVAILABLE, null).reason("Şu an müsait temsilci yok").build());
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
    online.forEach(p -> s.ringing.add(p.username()));
    calls.put(s.callId, s);
    busy.put(callerUserId, s.callId);

    withTenant(s.tenantId, () -> callLogService.createRinging(s.callId, callerUserId, callerUsername));
    scheduleTimeout(s);

    online.forEach(p -> sendSignal(p.username(), sig(CallSignalType.INCOMING_CALL, s.callId)
        .peerUserId(callerUserId).peerDisplayName(callerUsername).ringAll(true).build()));
    sendSignal(callerUsername, sig(CallSignalType.RINGING, s.callId).build());
  }

  // ─────────────────────────── Yaşam döngüsü ───────────────────────────

  /** Cevapla — atomic claim (ring-all'da ilk cevaplayan kazanır). */
  public void answer(UUID callId, Long answererUserId, String answererUsername) {
    boolean[] won = {false};
    CallSession s = calls.computeIfPresent(callId, (id, sess) -> {
      if (sess.status == CallStatus.RINGING && sess.ringing.contains(answererUsername)) {
        sess.status = CallStatus.ANSWERED;
        sess.calleeUserId = answererUserId;
        sess.calleeUsername = answererUsername;
        won[0] = true;
      }
      return sess;
    });
    if (s == null || !won[0]) {
      // Çağrı yok veya başkası kaptı → cevaplayanın zili kapansın
      sendSignal(answererUsername, sig(CallSignalType.CALL_TAKEN, callId).build());
      return;
    }
    cancelTimeout(s);
    busy.put(answererUserId, callId);

    // Ring-all: diğer çalanlara CALL_TAKEN
    s.ringing.stream().filter(u -> !u.equals(answererUsername))
        .forEach(u -> sendSignal(u, sig(CallSignalType.CALL_TAKEN, callId).build()));
    s.ringing.clear();
    s.ringing.add(answererUsername);

    withTenant(s.tenantId, () -> callLogService.markAnswered(callId, answererUserId));

    // İki tarafa ANSWERED — offer'ı ARAYAN üretir
    sendSignal(s.callerUsername, sig(CallSignalType.ANSWERED, callId)
        .peerUserId(answererUserId).peerDisplayName(answererUsername).build());
    sendSignal(answererUsername, sig(CallSignalType.ANSWERED, callId)
        .peerUserId(s.callerUserId).peerDisplayName(s.callerDisplayName).build());
  }

  /** Reddet. */
  public void reject(UUID callId, Long userId, String username) {
    CallSession s = calls.get(callId);
    if (s == null || s.status != CallStatus.RINGING) return;

    if (s.ringAll) {
      s.ringing.remove(username);
      if (s.ringing.isEmpty()) {
        terminate(s, CallStatus.REJECTED,
            () -> sendSignal(s.callerUsername, sig(CallSignalType.REJECTED, callId).build()));
      }
    } else {
      // Panel-panel: yalnız callee reddedebilir
      if (!username.equals(s.calleeUsername)) return;
      terminate(s, CallStatus.REJECTED,
          () -> sendSignal(s.callerUsername, sig(CallSignalType.REJECTED, callId).build()));
    }
  }

  /** İptal (yalnız arayan, çalarken). */
  public void cancel(UUID callId, Long userId) {
    CallSession s = calls.get(callId);
    if (s == null || !userId.equals(s.callerUserId) || s.status != CallStatus.RINGING) return;
    terminate(s, CallStatus.CANCELLED,
        () -> s.ringing.forEach(u -> sendSignal(u, sig(CallSignalType.CANCELLED, callId).build())));
  }

  /** Kapat (görüşme sırasında her iki taraf). Çalarken arayan kapatırsa iptal gibi. */
  public void hangup(UUID callId, Long userId, String username) {
    CallSession s = calls.get(callId);
    if (s == null) return;
    boolean participant = userId.equals(s.callerUserId) || userId.equals(s.calleeUserId);
    if (!participant) return;

    if (s.status == CallStatus.RINGING) {
      if (userId.equals(s.callerUserId)) {
        cancel(callId, userId);
      }
      return;
    }
    if (s.status == CallStatus.ANSWERED) {
      String other = username.equals(s.callerUsername) ? s.calleeUsername : s.callerUsername;
      terminate(s, CallStatus.COMPLETED,
          () -> sendSignal(other, sig(CallSignalType.HANGUP, callId).build()));
    }
  }

  /** SDP relay (opak) — yalnız çağrının iki tarafı, karşıya iletir. */
  public void relaySdp(UUID callId, Long fromUserId, DtoCallSdp sdp) {
    CallSession s = participantSession(callId, fromUserId);
    if (s == null) return;
    String target = fromUserId.equals(s.callerUserId) ? s.calleeUsername : s.callerUsername;
    if (target == null) return;
    sendSignal(target, sig(CallSignalType.SDP, callId).sdpType(sdp.getSdpType()).sdp(sdp.getSdp()).build());
  }

  /** ICE relay (opak). */
  public void relayIce(UUID callId, Long fromUserId, DtoCallIce ice) {
    CallSession s = participantSession(callId, fromUserId);
    if (s == null) return;
    String target = fromUserId.equals(s.callerUserId) ? s.calleeUsername : s.callerUsername;
    if (target == null) return;
    sendSignal(target, sig(CallSignalType.ICE, callId)
        .candidate(ice.getCandidate()).sdpMid(ice.getSdpMid()).sdpMLineIndex(ice.getSdpMLineIndex()).build());
  }

  // ─────────────────────────── Yardımcılar ───────────────────────────

  private CallSession participantSession(UUID callId, Long userId) {
    CallSession s = calls.get(callId);
    if (s == null) return null;
    boolean participant = userId.equals(s.callerUserId) || userId.equals(s.calleeUserId);
    return participant ? s : null;
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
      s.ringing.forEach(u -> sendSignal(u, sig(CallSignalType.TIMEOUT, callId).build()));
      sendSignal(s.callerUsername, sig(CallSignalType.TIMEOUT, callId).build());
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
      busy.remove(s.callerUserId, s.callId);
      if (s.calleeUserId != null) {
        busy.remove(s.calleeUserId, s.callId);
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

  private void sendSignal(String username, DtoCallSignal signal) {
    // [rtc-diag] geçici tanı logu — teslim edilen her sinyal + hedef username
    log.info("[rtc] SEND {} → user='{}' callId={}", signal.getType(), username, signal.getCallId());
    messagingTemplate.convertAndSendToUser(username, "/queue/rtc", signal);
  }

  private void sendError(String username, String reason) {
    sendSignal(username, sig(CallSignalType.ERROR, null).reason(reason).build());
  }

  private DtoCallSignal.DtoCallSignalBuilder sig(CallSignalType type, UUID callId) {
    return DtoCallSignal.builder().type(type).callId(callId);
  }
}
