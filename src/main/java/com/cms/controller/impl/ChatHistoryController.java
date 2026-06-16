package com.cms.controller.impl;

import com.cms.controller.IChatHistoryController;
import com.cms.dto.DtoChatMessage;
import com.cms.dto.DtoChatMessageSend;
import com.cms.service.IChatMessageService;
import com.cms.config.JwtAuthenticationFilter;
import com.cms.entity.ChatGroup;
import com.cms.entity.RootEntityResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.exception.UnauthorizedException;
import com.cms.repository.ChatGroupRepository;
import com.cms.service.ChatRateLimitService;
import com.cms.service.IFileService;
import com.cms.util.ChatTopics;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chat")
public class ChatHistoryController implements IChatHistoryController {

  private final IChatMessageService messageService;
  private final IFileService fileService;
  private final ChatGroupRepository chatGroupRepository;
  private final ChatRateLimitService rateLimitService;
  private final SimpMessagingTemplate messagingTemplate;

  /**
   * Admin REST üzerinden mesaj gönderme (AC veya TC).
   *
   * <p>Routing: TenantContext {@code JwtTenantFilter} tarafından URL path'inden
   * (TC: {@code /api/v1/chat/tenant/{tid}}) veya JWT claim'inden set edilir; bu yüzden
   * hangi DB'ye yazılacağı burada elle belirlenmez.
   *
   * <p>Broadcast: kaydedilen group'un {@code tenantId} alanına göre topic seçilir
   * ({@link ChatTopics}).
   */
  @Override
  @PostMapping({"/groups/{groupId}/messages", "/tenant/{tenantId}/groups/{groupId}/messages"})
  @PreAuthorize("hasAuthority('chat:read')")
  public ResponseEntity<RootEntityResponse<DtoChatMessage>> sendMessage(
      @PathVariable UUID groupId,
      @Valid @RequestBody DtoChatMessageSend payload) {
    Long userId = getCurrentUserId();
    rateLimitService.checkRateLimit(userId);

    DtoChatMessage saved = messageService.saveMessage(groupId, userId, getCurrentUsername(), payload);

    // Topic'i belirlemek için group'u oku (aynı DB context'te)
    ChatGroup group = chatGroupRepository.findById(groupId)
        .orElseThrow(() -> new ResourceNotFoundException("Chat group not found: " + groupId));
    String topic = ChatTopics.messageTopic(group);
    messagingTemplate.convertAndSend(topic, saved);
    log.debug("REST message broadcast to {}: msg={}", topic, saved.getId());

    return ResponseEntity.status(HttpStatus.CREATED).body(RootEntityResponse.ok(saved));
  }

  @Override
  @GetMapping({"/groups/{groupId}/messages", "/tenant/{tenantId}/groups/{groupId}/messages"})
  @PreAuthorize("hasAuthority('chat:read')")
  public ResponseEntity<RootEntityResponse<List<DtoChatMessage>>> getHistory(
      @PathVariable UUID groupId,
      @RequestParam(required = false) UUID before,
      @RequestParam(defaultValue = "50") int limit) {
    return ResponseEntity.ok(
        RootEntityResponse.ok(messageService.getHistory(groupId, getCurrentUserId(), before, limit)));
  }

  @Override
  @PutMapping({"/messages/{messageId}", "/tenant/{tenantId}/messages/{messageId}"})
  @PreAuthorize("hasAuthority('chat:read')")
  public ResponseEntity<RootEntityResponse<DtoChatMessage>> editMessage(
      @PathVariable UUID messageId, @RequestBody String newContent) {
    return ResponseEntity.ok(
        RootEntityResponse.ok(messageService.editMessage(messageId, getCurrentUserId(), newContent)));
  }

  @Override
  @DeleteMapping({"/messages/{messageId}", "/tenant/{tenantId}/messages/{messageId}"})
  @PreAuthorize("hasAuthority('chat:read')")
  public ResponseEntity<Void> deleteMessage(@PathVariable UUID messageId) {
    messageService.deleteMessage(messageId, getCurrentUserId());
    return ResponseEntity.noContent().build();
  }

  @Override
  @PostMapping({"/files", "/tenant/{tenantId}/files"})
  @PreAuthorize("hasAuthority('chat:read')")
  public ResponseEntity<RootEntityResponse<String>> uploadFile(@RequestParam("file") MultipartFile file) {
    String path = fileService.saveFile(file, "chat");
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(RootEntityResponse.ok(path));
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

  /** Gönderen adı — mesaja senderDisplayName olarak denormalize edilir (DB lookup'sız). */
  private String getCurrentUsername() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null
        && auth.getPrincipal() instanceof JwtAuthenticationFilter.CachedUserPrincipal principal) {
      return principal.getUsername();
    }
    return null;
  }
}
