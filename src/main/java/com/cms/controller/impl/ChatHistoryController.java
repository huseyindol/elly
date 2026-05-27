package com.cms.controller.impl;

import com.cms.controller.IChatHistoryController;
import com.cms.dto.DtoChatMessage;
import com.cms.service.IChatMessageService;
import com.cms.config.JwtAuthenticationFilter;
import com.cms.entity.RootEntityResponse;
import com.cms.exception.UnauthorizedException;
import com.cms.service.IFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chat")
public class ChatHistoryController implements IChatHistoryController {

  private final IChatMessageService messageService;
  private final IFileService fileService;

  @Override
  @GetMapping("/groups/{groupId}/messages")
  @PreAuthorize("hasAuthority('chat:read')")
  public ResponseEntity<RootEntityResponse<List<DtoChatMessage>>> getHistory(
      @PathVariable UUID groupId,
      @RequestParam(required = false) UUID before,
      @RequestParam(defaultValue = "50") int limit) {
    return ResponseEntity.ok(
        RootEntityResponse.ok(messageService.getHistory(groupId, getCurrentUserId(), before, limit)));
  }

  @Override
  @PutMapping("/messages/{messageId}")
  @PreAuthorize("hasAuthority('chat:read')")
  public ResponseEntity<RootEntityResponse<DtoChatMessage>> editMessage(
      @PathVariable UUID messageId, @RequestBody String newContent) {
    return ResponseEntity.ok(
        RootEntityResponse.ok(messageService.editMessage(messageId, getCurrentUserId(), newContent)));
  }

  @Override
  @DeleteMapping("/messages/{messageId}")
  @PreAuthorize("hasAuthority('chat:read')")
  public ResponseEntity<Void> deleteMessage(@PathVariable UUID messageId) {
    messageService.deleteMessage(messageId, getCurrentUserId());
    return ResponseEntity.noContent().build();
  }

  @Override
  @PostMapping("/files")
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
}
