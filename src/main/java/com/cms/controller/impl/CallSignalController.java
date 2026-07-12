package com.cms.controller.impl;

import java.util.UUID;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import com.cms.dto.DtoCallIce;
import com.cms.dto.DtoCallSdp;
import com.cms.dto.DtoCallStart;
import com.cms.dto.DtoTenantCallStart;
import com.cms.service.CallSessionService;
import com.cms.util.WsSessionUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * WebRTC sinyalleşme WS controller'ı. İş mantığı tamamen {@link CallSessionService}'te;
 * burada yalnız session attribute çözümü + yön (panel/tenant) ayrımı yapılır.
 * Yanıtlar {@code /user/queue/rtc} üzerinden {@link CallSessionService} tarafından gönderilir.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class CallSignalController {

  private final CallSessionService callSessionService;

  /** Panel-içi arama başlat. Yalnız panel oturumu (loginSource=admin). */
  @MessageMapping("/rtc/call")
  public void startCall(@Payload DtoCallStart payload, SimpMessageHeaderAccessor accessor) {
    Long userId = WsSessionUtils.requireUserId(accessor);
    String username = WsSessionUtils.username(accessor);
    String loginSource = WsSessionUtils.loginSource(accessor);
    // Panel araması yalnız panel oturumundan; tenant/guest ring-all endpoint'ini kullanır.
    // UI bu butonu tenant'a göstermez; yine de gelirse sessizce yok say.
    if (!"admin".equals(loginSource)) {
      log.warn("[rtc] Panel araması reddedildi (loginSource!=admin): user={} loginSource={}", username, loginSource);
      return;
    }
    callSessionService.startPanelCall(userId, username, WsSessionUtils.optionalTenant(accessor),
        payload.getCalleeUserId(), payload.getGroupId());
  }

  /** Tenant destek araması (ring-all). Yalnız tenant-site login (loginSource=tenant). */
  @MessageMapping("/tenant-rtc/{tenantId}/call")
  public void startTenantCall(@DestinationVariable String tenantId,
      @Payload(required = false) DtoTenantCallStart payload, SimpMessageHeaderAccessor accessor) {
    Long userId = WsSessionUtils.requireUserId(accessor);
    String username = WsSessionUtils.username(accessor);
    String sessionTenant = WsSessionUtils.optionalTenant(accessor);
    String loginSource = WsSessionUtils.loginSource(accessor);
    // Yalnız tenant-site kullanıcısı + path tenant == session tenant
    if (!"tenant".equals(loginSource) || sessionTenant == null || !sessionTenant.equals(tenantId)) {
      log.warn("[rtc] Tenant çağrısı reddedildi: user={} loginSource={} path={} session={}",
          username, loginSource, tenantId, sessionTenant);
      return;
    }
    callSessionService.startTenantRingAll(userId, username, sessionTenant,
        payload != null ? payload.getClientRef() : null);
  }

  @MessageMapping("/rtc/{callId}/answer")
  public void answer(@DestinationVariable UUID callId, SimpMessageHeaderAccessor accessor) {
    callSessionService.answer(callId, WsSessionUtils.requireUserId(accessor), WsSessionUtils.username(accessor));
  }

  @MessageMapping("/rtc/{callId}/reject")
  public void reject(@DestinationVariable UUID callId, SimpMessageHeaderAccessor accessor) {
    callSessionService.reject(callId, WsSessionUtils.requireUserId(accessor), WsSessionUtils.username(accessor));
  }

  @MessageMapping("/rtc/{callId}/cancel")
  public void cancel(@DestinationVariable UUID callId, SimpMessageHeaderAccessor accessor) {
    callSessionService.cancel(callId, WsSessionUtils.requireUserId(accessor));
  }

  @MessageMapping("/rtc/{callId}/hangup")
  public void hangup(@DestinationVariable UUID callId, SimpMessageHeaderAccessor accessor) {
    callSessionService.hangup(callId, WsSessionUtils.requireUserId(accessor), WsSessionUtils.username(accessor));
  }

  @MessageMapping("/rtc/{callId}/sdp")
  public void sdp(@DestinationVariable UUID callId, @Payload DtoCallSdp payload, SimpMessageHeaderAccessor accessor) {
    callSessionService.relaySdp(callId, WsSessionUtils.requireUserId(accessor), payload);
  }

  @MessageMapping("/rtc/{callId}/ice")
  public void ice(@DestinationVariable UUID callId, @Payload DtoCallIce payload, SimpMessageHeaderAccessor accessor) {
    callSessionService.relayIce(callId, WsSessionUtils.requireUserId(accessor), payload);
  }
}
