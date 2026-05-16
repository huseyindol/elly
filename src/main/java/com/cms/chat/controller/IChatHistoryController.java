package com.cms.chat.controller;

import com.cms.chat.dto.DtoChatMessage;
import com.cms.entity.RootEntityResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

@Tag(name = "Chat Messages", description = "Mesaj geçmişi ve yönetimi")
public interface IChatHistoryController {

  @Operation(summary = "Grup mesaj geçmişi (cursor-based pagination)")
  ResponseEntity<RootEntityResponse<List<DtoChatMessage>>> getHistory(UUID groupId, UUID before, int limit);

  @Operation(summary = "Mesajı düzenle")
  ResponseEntity<RootEntityResponse<DtoChatMessage>> editMessage(UUID messageId, String newContent);

  @Operation(summary = "Mesajı sil (soft delete)")
  ResponseEntity<Void> deleteMessage(UUID messageId);
}
