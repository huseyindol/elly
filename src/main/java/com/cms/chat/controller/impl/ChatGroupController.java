package com.cms.chat.controller.impl;

import com.cms.chat.controller.IChatGroupController;
import com.cms.chat.dto.DtoChatGroup;
import com.cms.chat.dto.DtoChatGroupCreate;
import com.cms.chat.dto.DtoChatMember;
import com.cms.chat.service.IChatGroupService;
import com.cms.entity.RootEntityResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ChatGroupController implements IChatGroupController {

  private final IChatGroupService groupService;

  @Override
  @PreAuthorize("hasAuthority('chat:read')")
  public ResponseEntity<RootEntityResponse<DtoChatGroup>> createGroup(
      DtoChatGroupCreate dto) {
    Long userId = getCurrentUserId();
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(RootEntityResponse.ok(groupService.createGroup(dto, userId)));
  }

  @Override
  @PreAuthorize("hasAuthority('chat:read')")
  public ResponseEntity<RootEntityResponse<List<DtoChatGroup>>> getMyGroups() {
    return ResponseEntity.ok(RootEntityResponse.ok(groupService.getMyGroups(getCurrentUserId())));
  }

  @Override
  @PreAuthorize("hasAuthority('chat:read')")
  public ResponseEntity<RootEntityResponse<DtoChatGroup>> getGroup(UUID groupId) {
    return ResponseEntity.ok(RootEntityResponse.ok(groupService.getGroupById(groupId, getCurrentUserId())));
  }

  @Override
  @PreAuthorize("hasAuthority('chat:read')")
  public ResponseEntity<RootEntityResponse<DtoChatGroup>> getOrCreateDm(Long targetUserId) {
    return ResponseEntity.ok(RootEntityResponse.ok(groupService.getOrCreateDm(getCurrentUserId(), targetUserId)));
  }

  @Override
  @PreAuthorize("hasAuthority('chat:manage')")
  public ResponseEntity<RootEntityResponse<DtoChatMember>> addMember(UUID groupId, Long userId) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(RootEntityResponse.ok(groupService.addMember(groupId, userId, getCurrentUserId())));
  }

  @Override
  @PreAuthorize("hasAuthority('chat:manage')")
  public ResponseEntity<Void> removeMember(UUID groupId, Long userId) {
    groupService.removeMember(groupId, userId, getCurrentUserId());
    return ResponseEntity.noContent().build();
  }

  @Override
  @PreAuthorize("hasAuthority('chat:read')")
  public ResponseEntity<RootEntityResponse<List<DtoChatMember>>> getMembers(UUID groupId) {
    return ResponseEntity.ok(RootEntityResponse.ok(groupService.getMembers(groupId, getCurrentUserId())));
  }

  @Override
  @PreAuthorize("hasAuthority('chat:manage')")
  public ResponseEntity<Void> deleteGroup(UUID groupId) {
    groupService.deleteGroup(groupId, getCurrentUserId());
    return ResponseEntity.noContent().build();
  }

  private Long getCurrentUserId() {
    var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || auth.getPrincipal() == null) {
      throw new com.cms.exception.UnauthorizedException("Not authenticated");
    }
    if (auth.getPrincipal() instanceof com.cms.config.JwtAuthenticationFilter.CachedUserPrincipal principal) {
      return principal.getUserId();
    }
    throw new com.cms.exception.UnauthorizedException("Cannot resolve user identity");
  }
}
