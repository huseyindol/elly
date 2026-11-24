package com.cms.controller.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.socket.messaging.SessionConnectEvent;

import com.cms.config.TenantContext;
import com.cms.dto.DtoAuthResponse;
import com.cms.dto.DtoLogin;
import com.cms.service.ChatPresenceService;
import com.cms.service.IAuthService;
import com.cms.util.AuthCookieWriter;
import com.cms.util.JwtUtil;

/**
 * İncelemenin işaret ettiği iki bağlantı noktası sabitlenir: oturum kimliği ve tenant servis metotlarına
 * gerçekten iletiliyor mu (yoksa tazeleme sessizce devre dışı kalır), ve giriş sayaç anahtarı gerçekten
 * Cloudflare başlığından mı geliyor (yoksa tüm site tek sayaça döner).
 */
class AuthAndPresenceWiringTest {

  @AfterEach
  void clearTenant() {
    TenantContext.clear();
  }

  private static Message<byte[]> sessionMessage(StompCommand command, Map<String, Object> attributes) {
    StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
    accessor.setSessionId("sess-1");
    accessor.setSessionAttributes(attributes);
    return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
  }

  @Test
  void presenceIsRecordedPerTenantWithTheRealSessionId() {
    ChatPresenceService presence = mock(ChatPresenceService.class);
    ChatWebSocketController controller =
        new ChatWebSocketController(null, null, presence, null, null, null);
    Map<String, Object> attributes = Map.of("userId", 4L, "username", "ali", "tenantId", "tenant1");

    controller.handleSessionConnect(new SessionConnectEvent(this,
        sessionMessage(StompCommand.CONNECT, attributes)));

    verify(presence).userConnected("tenant1", 4L, "sess-1");
  }

  @Test
  void loginCounterKeyComesFromCloudflareHeaderNotFromTheProxyAddress() {
    IAuthService authService = mock(IAuthService.class);
    when(authService.login(any(DtoLogin.class), anyString())).thenReturn(new DtoAuthResponse());
    AuthController controller = new AuthController(authService, mock(JwtUtil.class),
        mock(AuthCookieWriter.class));
    DtoLogin login = new DtoLogin();
    login.setUsernameOrEmail("someone");
    login.setPassword("wrong-password");
    login.setTenantId("tenant1");
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRemoteAddr("10.42.0.1");
    request.addHeader("X-Forwarded-For", "10.42.0.1");
    request.addHeader("CF-Connecting-IP", "88.230.95.83");

    controller.login(login, null, request);

    verify(authService).login(any(DtoLogin.class), eq("88.230.95.83"));
  }
}
