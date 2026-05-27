package com.cms.config;

import com.cms.dto.DtoChatWsError;
import com.cms.exception.ForbiddenException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.web.bind.annotation.ControllerAdvice;

/**
 * WebSocket {@code @MessageMapping} handler'larından fırlayan hataları
 * gönderen kullanıcının {@code /user/queue/chat-errors} kuyruğuna iletir.
 */
@ControllerAdvice
@Slf4j
public class ChatWebSocketExceptionHandler {

  @MessageExceptionHandler(ForbiddenException.class)
  @SendToUser("/queue/chat-errors")
  public DtoChatWsError handleForbidden(ForbiddenException ex) {
    log.debug("WebSocket forbidden: {} ({})", ex.getMessage(), ex.getErrorCode());
    return new DtoChatWsError(ex.getErrorCode(), ex.getMessage(), null);
  }

  @MessageExceptionHandler(Exception.class)
  @SendToUser("/queue/chat-errors")
  public DtoChatWsError handleGeneric(Exception ex) {
    log.warn("WebSocket handler error: {}", ex.getMessage());
    return new DtoChatWsError("CHAT_ERROR", ex.getMessage(), null);
  }
}
