package com.cms.chat.controller.impl;

import com.cms.chat.controller.IChatHistoryController;
import com.cms.chat.dto.DtoChatMessage;
import com.cms.chat.service.IChatMessageService;
import com.cms.config.JwtAuthenticationFilter;
import com.cms.entity.RootEntityResponse;
import com.cms.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ChatHistoryController implements IChatHistoryController {

  private final IChatMessageService messageService;

  @Override
  @PreAuthorize("hasAuthority('chat:read')")
  public ResponseEntity<RootEntityResponse<List<DtoChatMessage>>> getHistory(
      UUID groupId, UUID before, int limit) {
    return ResponseEntity.ok(
        RootEntityResponse.ok(messageService.getHistory(groupId, getCurrentUserId(), before, limit)));
  }

  @Override
  @PreAuthorize("hasAuthority('chat:manage')")
  public ResponseEntity<RootEntityResponse<DtoChatMessage>> editMessage(UUID messageId, String newContent) {
    return ResponseEntity.ok(
        RootEntityResponse.ok(messageService.editMessage(messageId, getCurrentUserId(), newContent)));
  }

  @Override
  @PreAuthorize("hasAuthority('chat:manage')")
  public ResponseEntity<Void> deleteMessage(UUID messageId) {
    messageService.deleteMessage(messageId, getCurrentUserId());
    return ResponseEntity.noContent().build();
  }

  private Long getCurrentUserId() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || auth.getPrincipal() == null) {
      throw new UnauthorizedException("Not authenticated");
    }
    if (auth.getPrincipal() instanceof JwtAuthenticationFilter.CachedUserPrincipal principal) {
      return principal.getUserId();
    }
    throw new UnauthorizedException("Cannot resolve user identity");
  }
}
