package com.cms.chat.controller;

import com.cms.chat.dto.DtoChatMessage;
import com.cms.entity.RootEntityResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Chat Messages", description = "Mesaj geçmişi ve yönetimi")
@RequestMapping("/api/v1/chat")
public interface IChatHistoryController {

  @Operation(summary = "Grup mesaj geçmişi (cursor-based pagination)")
  @GetMapping("/groups/{groupId}/messages")
  ResponseEntity<RootEntityResponse<List<DtoChatMessage>>> getHistory(
      @PathVariable UUID groupId,
      @RequestParam(required = false) UUID before,
      @RequestParam(defaultValue = "50") int limit);

  @Operation(summary = "Mesajı düzenle")
  @PutMapping("/messages/{messageId}")
  ResponseEntity<RootEntityResponse<DtoChatMessage>> editMessage(
      @PathVariable UUID messageId,
      @RequestBody String newContent);

  @Operation(summary = "Mesajı sil (soft delete)")
  @DeleteMapping("/messages/{messageId}")
  ResponseEntity<Void> deleteMessage(@PathVariable UUID messageId);
}
