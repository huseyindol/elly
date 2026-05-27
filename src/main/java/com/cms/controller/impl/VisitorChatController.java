package com.cms.controller.impl;

import com.cms.config.JwtAuthenticationFilter;
import com.cms.controller.IVisitorChatController;
import com.cms.dto.DtoChatGroup;
import com.cms.dto.DtoChatMessage;
import com.cms.dto.DtoChatMessageSend;
import com.cms.dto.DtoVisitorIdentity;
import com.cms.entity.RootEntityResponse;
import com.cms.exception.UnauthorizedException;
import com.cms.service.IVisitorChatService;
import com.cms.service.IVisitorIdentityResolver;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Tenant Chat (TC) için kayıtlı tenant user'larına yönelik endpoint'ler.
 *
 * <p>TenantContext: JwtTenantFilter, tenant JWT'sinin tenantId claim'inden
 * (veya X-Tenant-Id header'ından) okuyup set eder. Bu endpoint'ler `loginSource=tenant`
 * için tasarlandı ama admin JWT'siyle de çağrılabilir (X-Tenant-Id header'ı ile).
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/tenant-chat")
@RequiredArgsConstructor
public class VisitorChatController implements IVisitorChatController {

  private final IVisitorChatService visitorChatService;
  private final IVisitorIdentityResolver visitorIdentityResolver;

  @Override
  @PostMapping("/session")
  @PreAuthorize("isAuthenticated()")
  @ResponseStatus(HttpStatus.OK)
  public ResponseEntity<RootEntityResponse<DtoVisitorIdentity>> ensureSession() {
    DtoVisitorIdentity identity = visitorIdentityResolver.ensureForCurrentUser();
    return ResponseEntity.ok(RootEntityResponse.ok(identity));
  }

  @Override
  @GetMapping("/groups")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<RootEntityResponse<List<DtoChatGroup>>> listVisibleGroups() {
    return ResponseEntity.ok(RootEntityResponse.ok(visitorChatService.listVisibleGroups()));
  }

  @Override
  @GetMapping("/groups/{groupId}/messages")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<RootEntityResponse<List<DtoChatMessage>>> getHistory(
      @PathVariable UUID groupId,
      @RequestParam(required = false) UUID before,
      @RequestParam(defaultValue = "50") int limit) {
    return ResponseEntity.ok(
        RootEntityResponse.ok(visitorChatService.getHistory(groupId, before, limit)));
  }

  @Override
  @PostMapping("/groups/{groupId}/messages")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<RootEntityResponse<DtoChatMessage>> sendMessage(
      @PathVariable UUID groupId,
      @Valid @RequestBody DtoChatMessageSend payload) {
    DtoVisitorIdentity visitor = visitorIdentityResolver.ensureForCurrentUser();
    DtoChatMessage saved = visitorChatService.sendMessage(groupId, visitor.getId(), payload);
    return ResponseEntity.status(HttpStatus.CREATED).body(RootEntityResponse.ok(saved));
  }

  // Authentication context helper'ı - unused olabilir ama UnauthorizedException
  // mantığını burada tutmak için import'lu bırakıldı.
  @SuppressWarnings("unused")
  private static void ensureAuthenticated() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || auth.getPrincipal() == null) {
      throw new UnauthorizedException("Not authenticated");
    }
    if (!(auth.getPrincipal() instanceof JwtAuthenticationFilter.CachedUserPrincipal)) {
      throw new UnauthorizedException("Cannot resolve user identity");
    }
  }
}
