package com.cms.chat.controller.impl;

import com.cms.chat.controller.IChatGroupController;
import com.cms.chat.dto.DtoChatGroup;
import com.cms.chat.dto.DtoChatGroupCreate;
import com.cms.chat.dto.DtoChatMember;
import com.cms.chat.service.IChatGroupService;
import com.cms.entity.RootEntityResponse;
import com.cms.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chat")
public class ChatGroupController implements IChatGroupController {

  private final IChatGroupService groupService;
  private final SimpMessagingTemplate messagingTemplate;

  @Override
  @PostMapping("/groups")
  @PreAuthorize("hasAuthority('chat:read')")
  public ResponseEntity<RootEntityResponse<DtoChatGroup>> createGroup(
      @RequestBody DtoChatGroupCreate dto) {
    DtoChatGroup created = groupService.createGroup(dto, getCurrentUserId());
    // Tüm bağlı kullanıcılara yayınla; frontend visibilityLevel'a göre filtreler
    messagingTemplate.convertAndSend("/topic/groups/new", created);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(RootEntityResponse.ok(created));
  }

  @Override
  @GetMapping("/groups")
  @PreAuthorize("hasAuthority('chat:read')")
  public ResponseEntity<RootEntityResponse<List<DtoChatGroup>>> getMyGroups() {
    return ResponseEntity.ok(RootEntityResponse.ok(groupService.getMyGroups(getCurrentUserId())));
  }

  @Override
  @GetMapping("/groups/{groupId}")
  @PreAuthorize("hasAuthority('chat:read')")
  public ResponseEntity<RootEntityResponse<DtoChatGroup>> getGroup(@PathVariable UUID groupId) {
    return ResponseEntity.ok(RootEntityResponse.ok(groupService.getGroupById(groupId, getCurrentUserId())));
  }

  @Override
  @PostMapping("/dm/{targetUserId}")
  @PreAuthorize("hasAuthority('chat:read')")
  public ResponseEntity<RootEntityResponse<DtoChatGroup>> getOrCreateDm(@PathVariable Long targetUserId) {
    return ResponseEntity.ok(RootEntityResponse.ok(groupService.getOrCreateDm(getCurrentUserId(), targetUserId)));
  }

  @Override
  @PostMapping("/groups/{groupId}/members/{userId}")
  @PreAuthorize("hasAuthority('chat:manage')")
  public ResponseEntity<RootEntityResponse<DtoChatMember>> addMember(
      @PathVariable UUID groupId, @PathVariable Long userId) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(RootEntityResponse.ok(groupService.addMember(groupId, userId, getCurrentUserId())));
  }

  @Override
  @DeleteMapping("/groups/{groupId}/members/{userId}")
  @PreAuthorize("hasAuthority('chat:manage')")
  public ResponseEntity<Void> removeMember(@PathVariable UUID groupId, @PathVariable Long userId) {
    groupService.removeMember(groupId, userId, getCurrentUserId());
    return ResponseEntity.noContent().build();
  }

  @Override
  @GetMapping("/groups/{groupId}/members")
  @PreAuthorize("hasAuthority('chat:read')")
  public ResponseEntity<RootEntityResponse<List<DtoChatMember>>> getMembers(@PathVariable UUID groupId) {
    return ResponseEntity.ok(RootEntityResponse.ok(groupService.getMembers(groupId, getCurrentUserId())));
  }

  @Override
  @DeleteMapping("/groups/{groupId}")
  @PreAuthorize("hasAuthority('chat:manage')")
  public ResponseEntity<Void> deleteGroup(@PathVariable UUID groupId) {
    groupService.deleteGroup(groupId, getCurrentUserId());
    messagingTemplate.convertAndSend("/topic/groups/deleted", groupId.toString());
    return ResponseEntity.noContent().build();
  }

  private Long getCurrentUserId() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || auth.getPrincipal() == null) {
      throw new UnauthorizedException("Not authenticated");
    }
    if (auth.getPrincipal() instanceof com.cms.config.JwtAuthenticationFilter.CachedUserPrincipal principal) {
      return principal.getUserId();
    }
    throw new UnauthorizedException("Cannot resolve user identity");
  }
}
