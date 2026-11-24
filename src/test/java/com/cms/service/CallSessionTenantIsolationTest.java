package com.cms.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.TaskScheduler;

import com.cms.dto.DtoCallIce;
import com.cms.dto.DtoCallSdp;
import com.cms.dto.DtoCallSignal;
import com.cms.enums.CallSignalType;
import com.cms.enums.CallStatus;
import com.cms.service.CallLogService.PanelUserRef;
import com.cms.service.CallLogService.UserCallInfo;

/**
 * Çağrı sinyalleşmesinde tenant izolasyonu. Kullanıcı numaraları her tenant veritabanında ayrı sayılır:
 * başka tenant'taki aynı numaralı kullanıcı ne sinyalleri alabilmeli ne de çağrının katılımcısı gibi
 * işlem yapabilmeli.
 */
class CallSessionTenantIsolationTest {

  private static final Long CALLER = 1L;
  private static final Long CALLEE = 2L;
  private static final Long VISITOR = 5L;   // tenant-site çağıran (numara olarak)
  private static final Long AGENT = 7L;     // tenant t1'de panel temsilcisi
  private static final Long OTHER = 9L;     // tenant t1'de başka panel kullanıcısı
  private static final String TENANT = "t1";
  private static final String OTHER_TENANT = "t2";
  private static final String CALLEE_TOPIC = "/topic/tenant/t1/user/2/rtc";
  private static final String VISITOR_TOPIC = "/topic/tenant/t1/user/5/rtc";

  private SimpMessagingTemplate messaging;
  private CallLogService callLog;
  private ChatPresenceService presence;
  private TaskScheduler scheduler;
  private CallSessionService service;

  @BeforeEach
  void setUp() {
    messaging = mock(SimpMessagingTemplate.class);
    callLog = mock(CallLogService.class);
    presence = mock(ChatPresenceService.class);
    scheduler = mock(TaskScheduler.class);
    service = new CallSessionService(messaging, presence, callLog, scheduler);

    when(presence.isOnline(TENANT, CALLEE)).thenReturn(true);
    when(callLog.lookupUser(CALLER)).thenReturn(Optional.of(new UserCallInfo(CALLER, "arayan", 4, false)));
    when(callLog.lookupUser(CALLEE)).thenReturn(Optional.of(new UserCallInfo(CALLEE, "karsi", 1, false)));
  }

  /** Arayan 1 → aranan 2 (tenant t1); dönen değer çağrı kimliği. */
  private UUID placeCall() {
    service.startPanelCall(CALLER, "arayan", TENANT, CALLEE, null);
    ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
    verify(messaging).convertAndSend(eq(CALLEE_TOPIC), captor.capture());
    DtoCallSignal incoming = (DtoCallSignal) captor.getValue();
    assertEquals(CallSignalType.INCOMING_CALL, incoming.getType());
    return incoming.getCallId();
  }

  /** Belirli bir konuya giden çağrı sinyali sayısı. */
  private long topicSends(String destination) {
    return mockingDetails(messaging).getInvocations().stream()
        .filter(inv -> "convertAndSend".equals(inv.getMethod().getName())
            && inv.getArguments().length >= 2
            && destination.equals(inv.getArgument(0))
            && inv.getArgument(1) instanceof DtoCallSignal)
        .count();
  }

  @Test
  void signalsGoToTheTenantScopedTopicAndTheTenantQualifiedUserQueue() {
    placeCall();

    // Aranana gelen çağrı ve arayana "çalıyor": tenant'lı konular.
    assertEquals(1, topicSends("/topic/tenant/t1/user/2/rtc"));
    assertEquals(1, topicSends("/topic/tenant/t1/user/1/rtc"));
    // Kullanıcı hedefli kuyruk tenant'lı anahtarla: "karsi"/"arayan" adlı başka tenant kullanıcısına düşmez.
    verify(messaging).convertAndSendToUser(eq("t1:karsi"), eq("/queue/rtc"), any(DtoCallSignal.class));
    verify(messaging).convertAndSendToUser(eq("t1:arayan"), eq("/queue/rtc"), any(DtoCallSignal.class));
    verify(messaging, never()).convertAndSendToUser(eq("karsi"), anyString(), any(Object.class));
    verify(messaging, never()).convertAndSendToUser(eq("arayan"), anyString(), any(Object.class));
  }

  @Test
  void legacyTenantLessTopicIsNoLongerFed() {
    placeCall();

    // Eski tenant'sız konu başka tenant'taki aynı numaralı kullanıcıya da açık olduğundan kapatıldı:
    // oraya artık hiçbir sinyal yazılmaz (eski istemciler oradan hiçbir şey alamaz — sessiz, bağlantı kopmaz).
    assertEquals(0, topicSends("/topic/user/2/rtc"));
    assertEquals(0, topicSends("/topic/user/1/rtc"));
  }

  @Test
  void sdpAndIceFromAnotherTenantAreIgnored() {
    UUID callId = placeCall();
    long before = topicSends(CALLEE_TOPIC);

    // Aynı numara (1) ama başka tenant: katılımcı sayılmaz.
    service.relaySdp(callId, CALLER, OTHER_TENANT, new DtoCallSdp("offer", "v=0"));
    service.relayIce(callId, CALLER, OTHER_TENANT, new DtoCallIce("c", "0", 0));
    assertEquals(before, topicSends(CALLEE_TOPIC));

    // Doğru tenant: SDP karşıya iletilir.
    service.relaySdp(callId, CALLER, TENANT, new DtoCallSdp("offer", "v=0"));
    assertEquals(before + 1, topicSends(CALLEE_TOPIC));
  }

  @Test
  void answeringFromAnotherTenantDoesNotClaimTheCall() {
    UUID callId = placeCall();
    long victimSends = topicSends(CALLEE_TOPIC);

    // Başka tenant'taki 2 numaralı kullanıcı cevaplamaya kalkar: çağrı çalmaya devam eder.
    service.answer(callId, CALLEE, "yabanci", OTHER_TENANT);
    verify(callLog, never()).markAnswered(any(), any());
    // "Çağrı alındı" sinyali yabancının KENDİ tenant'ına gider — asıl arananın (t1'deki 2) zili susturulmaz.
    assertEquals(victimSends, topicSends(CALLEE_TOPIC), "kurban tenant'ındaki arananın konusuna sinyal gitmemeli");
    assertEquals(1, topicSends("/topic/tenant/t2/user/2/rtc"));

    // Gerçek aranan cevaplar: ANSWERED.
    service.answer(callId, CALLEE, "karsi", TENANT);
    verify(callLog).markAnswered(callId, CALLEE);
  }

  @Test
  void endingTheCallFromAnotherTenantIsIgnored() {
    UUID callId = placeCall();

    service.cancel(callId, CALLER, OTHER_TENANT);
    service.reject(callId, CALLEE, OTHER_TENANT);
    verify(callLog, never()).markTerminal(any(), any(CallStatus.class));

    // Çağrı hâlâ açık: doğru tenant'tan reddedilebilir.
    service.reject(callId, CALLEE, TENANT);
    verify(callLog).markTerminal(callId, CallStatus.REJECTED);
  }

  @Test
  void answerToAnUnknownCallUsesTheAnswerersOwnTenant() {
    UUID unknown = UUID.randomUUID();

    service.answer(unknown, CALLEE, "karsi", TENANT);

    // Çağrı kaydı yok: tenant cevaplayanın oturumundan gelir, CALL_TAKEN kendi tenant konusuna gider.
    assertEquals(1, topicSends("/topic/tenant/t1/user/2/rtc"));
    assertEquals(0, topicSends("/topic/tenant/t2/user/2/rtc"));
  }

  // ── "Meşgul" ve clientRef yansıması: anahtarlar tenant'lı olmalı ──

  @Test
  void beingInACallInOneTenantDoesNotMakeTheSameNumberBusyInAnother() {
    placeCall(); // tenant t1: 1 ve 2 bir çağrıda

    // Başka tenant'taki 1 numara, o tenant'taki 2'yi arar: t1'deki çağrı onu meşgul saymamalı.
    when(presence.isOnline(OTHER_TENANT, CALLEE)).thenReturn(true);
    String foreignCaller = "/topic/tenant/t2/user/1/rtc";

    service.startPanelCall(CALLER, "diger", OTHER_TENANT, CALLEE, null);

    // Aranan INCOMING_CALL aldı, arayan tek bir sinyal aldı ve o sinyal RINGING (BUSY değil).
    assertEquals(1, topicSends("/topic/tenant/t2/user/2/rtc"));
    assertEquals(1, topicSends(foreignCaller));
    ArgumentCaptor<Object> toCaller = ArgumentCaptor.forClass(Object.class);
    verify(messaging).convertAndSend(eq(foreignCaller), toCaller.capture());
    assertEquals(CallSignalType.RINGING, ((DtoCallSignal) toCaller.getValue()).getType());
    verify(callLog, times(2)).createRinging(any(UUID.class), eq(CALLER), any());
  }

  @Test
  void hangupEndsTheCallOnlyFromTheCallsOwnTenant() {
    UUID callId = placeCall();
    long calleeSends = topicSends(CALLEE_TOPIC);

    service.hangup(callId, CALLER, "arayan", OTHER_TENANT); // RINGING'de arayan: iptal gibi — yabancı tenant
    verify(callLog, never()).markTerminal(any(), any(CallStatus.class));

    service.cancel(callId, CALLER, TENANT);
    verify(callLog).markTerminal(callId, CallStatus.CANCELLED);
    assertEquals(calleeSends + 1, topicSends(CALLEE_TOPIC)); // CANCELLED karşıya gider
  }

  @Test
  void ringTimeoutSignalsBothSidesInTheirOwnTenant() {
    UUID callId = placeCall();
    long calleeSends = topicSends(CALLEE_TOPIC);
    ArgumentCaptor<Runnable> timeout = ArgumentCaptor.forClass(Runnable.class);
    verify(scheduler).schedule(timeout.capture(), any(Date.class));

    timeout.getValue().run(); // 30 sn dolmuş gibi

    verify(callLog).markTerminal(callId, CallStatus.MISSED);
    assertEquals(calleeSends + 1, topicSends(CALLEE_TOPIC));
    assertEquals(2, topicSends("/topic/tenant/t1/user/1/rtc")); // RINGING + TIMEOUT
    assertEquals(0, topicSends("/topic/tenant/t2/user/1/rtc"));
  }

  @Test
  void clientRefMirrorReachesOnlyTheCallersOwnTenantSignals() {
    stubVisitorWorld();
    when(callLog.findPanelUsers()).thenReturn(List.of(new PanelUserRef(AGENT, "temsilci7")));

    // t1'deki 5 numaralı ziyaretçi destek çağrısı başlatır: sinyalleri ref-A yansımasından da alır.
    service.startTenantRingAll(VISITOR, "ziyaretci", TENANT, "ref-A");
    String mirror = "/topic/rtc-client/ref-A/rtc";
    long mirrored = topicSends(mirror);
    assertEquals(1, mirrored, "RINGING yansıması");
    assertEquals(1, topicSends("/topic/tenant/t1/user/7/rtc"), "temsilciye INCOMING_CALL");

    // Başka tenant'taki 5 numaralı kullanıcı için giden sinyal bu yansıtmaya DÜŞMEMELİ.
    when(presence.isOnline(OTHER_TENANT, VISITOR)).thenReturn(true);
    service.startPanelCall(OTHER, "admin9", OTHER_TENANT, VISITOR, null);

    assertEquals(1, topicSends("/topic/tenant/t2/user/5/rtc"), "yabancı tenant'taki 5 kendi konusunu alır");
    assertEquals(mirrored, topicSends(mirror), "yansıtmaya yabancı tenant sinyali gitmedi");
  }

  @Test
  void clientRefMirrorIsReleasedWhenTheSupportCallNeverStarts() {
    stubVisitorWorld();
    when(callLog.findPanelUsers()).thenReturn(List.of()); // çevrimiçi temsilci yok

    service.startTenantRingAll(VISITOR, "ziyaretci", TENANT, "ref-B");
    String mirror = "/topic/rtc-client/ref-B/rtc";
    long mirrored = topicSends(mirror);
    assertEquals(1, mirrored, "UNAVAILABLE yansıması (çağrı kurulmadan önce)");
    long visitorSends = topicSends(VISITOR_TOPIC);

    // Çağrı kurulmadı: sonrasında 5'e giden sinyaller artık ref-B'ye yansıtılmaz.
    service.startPanelCall(OTHER, "admin9", TENANT, VISITOR, null);

    assertEquals(visitorSends + 1, topicSends(VISITOR_TOPIC), "5 kendi tenant konusundan INCOMING_CALL aldı");
    assertEquals(mirrored, topicSends(mirror));
  }

  @Test
  void ringAllFlowAnswerAndHangup() {
    stubVisitorWorld();
    when(callLog.findPanelUsers()).thenReturn(List.of(new PanelUserRef(AGENT, "temsilci7")));

    service.startTenantRingAll(VISITOR, "ziyaretci", TENANT, "ref-C");
    ArgumentCaptor<Object> signal = ArgumentCaptor.forClass(Object.class);
    verify(messaging).convertAndSend(eq("/topic/tenant/t1/user/7/rtc"), signal.capture());
    DtoCallSignal incoming = (DtoCallSignal) signal.getValue();
    UUID ringCall = incoming.getCallId();
    assertTrue(incoming.isRingAll());
    verify(callLog).createRinging(eq(ringCall), eq(VISITOR), any());

    // Temsilci cevaplar → ANSWERED (yabancı tenant'tan aynı numara: yok sayılır); kapatır → HANGUP + COMPLETED.
    service.answer(ringCall, AGENT, "temsilci7", OTHER_TENANT);
    verify(callLog, never()).markAnswered(any(), any());
    service.answer(ringCall, AGENT, "temsilci7", TENANT);
    verify(callLog).markAnswered(ringCall, AGENT);

    service.hangup(ringCall, AGENT, "temsilci7", TENANT);
    verify(callLog).markTerminal(ringCall, CallStatus.COMPLETED);

    // Meşgullük bitti: ziyaretçi yeniden arayabilir.
    service.startTenantRingAll(VISITOR, "ziyaretci", TENANT, "ref-D");
    verify(callLog, times(2)).createRinging(any(UUID.class), eq(VISITOR), any());
  }

  private void stubVisitorWorld() {
    when(callLog.lookupUser(VISITOR)).thenReturn(Optional.of(new UserCallInfo(VISITOR, "u5", 1, false)));
    when(callLog.lookupUser(OTHER)).thenReturn(Optional.of(new UserCallInfo(OTHER, "admin9", 4, false)));
    when(callLog.lookupUser(AGENT)).thenReturn(Optional.of(new UserCallInfo(AGENT, "temsilci7", 1, false)));
    when(presence.isOnline(TENANT, VISITOR)).thenReturn(true);
    when(presence.isOnline(TENANT, AGENT)).thenReturn(true);
  }
}
