package com.cms.controller;

import com.cms.dto.DtoChatGroup;
import com.cms.dto.DtoChatMessage;
import com.cms.dto.DtoChatMessageSend;
import com.cms.dto.DtoVisitorIdentity;
import com.cms.entity.RootEntityResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

/**
 * Tenant Chat (TC) — kayıtlı tenant user'ları için endpoint'ler.
 * <p>Path: {@code /api/v1/tenant-chat/*} — admin chat endpoint'lerinden ayrı tutulur.
 * Auth: JWT (loginSource=tenant veya admin); regular JwtAuthenticationFilter zinciri.
 */
@Tag(name = "Tenant Chat (Visitor)", description = "Kayıtlı website user'ları için chat erişimi")
public interface IVisitorChatController {

  @Operation(summary = "Kendi tenant user'ım için VisitorIdentity al/yarat")
  ResponseEntity<RootEntityResponse<DtoVisitorIdentity>> ensureSession();

  @Operation(summary = "Bu tenant'ta visitor erişimine açık chat grupları")
  ResponseEntity<RootEntityResponse<List<DtoChatGroup>>> listVisibleGroups();

  @Operation(summary = "Grup mesaj geçmişi (cursor pagination)")
  ResponseEntity<RootEntityResponse<List<DtoChatMessage>>> getHistory(UUID groupId, UUID before, int limit);

  @Operation(summary = "Visitor olarak mesaj yaz")
  ResponseEntity<RootEntityResponse<DtoChatMessage>> sendMessage(UUID groupId, DtoChatMessageSend payload);
}
