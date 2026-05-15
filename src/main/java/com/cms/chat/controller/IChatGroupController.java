package com.cms.chat.controller;

import com.cms.chat.dto.DtoChatGroup;
import com.cms.chat.dto.DtoChatGroupCreate;
import com.cms.chat.dto.DtoChatMember;
import com.cms.entity.RootEntityResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Chat Groups", description = "Admin panel chat grup yönetimi")
@RequestMapping("/api/v1/chat")
public interface IChatGroupController {

  @Operation(summary = "Yeni grup oluştur")
  @PostMapping("/groups")
  ResponseEntity<RootEntityResponse<DtoChatGroup>> createGroup(@Valid @RequestBody DtoChatGroupCreate dto);

  @Operation(summary = "Katıldığım grupları listele")
  @GetMapping("/groups")
  ResponseEntity<RootEntityResponse<List<DtoChatGroup>>> getMyGroups();

  @Operation(summary = "Grup detayı")
  @GetMapping("/groups/{groupId}")
  ResponseEntity<RootEntityResponse<DtoChatGroup>> getGroup(@PathVariable UUID groupId);

  @Operation(summary = "DM getir veya oluştur")
  @PostMapping("/dm/{targetUserId}")
  ResponseEntity<RootEntityResponse<DtoChatGroup>> getOrCreateDm(@PathVariable Long targetUserId);

  @Operation(summary = "Gruba üye ekle")
  @PostMapping("/groups/{groupId}/members/{userId}")
  ResponseEntity<RootEntityResponse<DtoChatMember>> addMember(
      @PathVariable UUID groupId,
      @PathVariable Long userId);

  @Operation(summary = "Gruptan üye çıkar")
  @DeleteMapping("/groups/{groupId}/members/{userId}")
  ResponseEntity<Void> removeMember(
      @PathVariable UUID groupId,
      @PathVariable Long userId);

  @Operation(summary = "Grup üyelerini listele")
  @GetMapping("/groups/{groupId}/members")
  ResponseEntity<RootEntityResponse<List<DtoChatMember>>> getMembers(@PathVariable UUID groupId);

  @Operation(summary = "Grubu sil")
  @DeleteMapping("/groups/{groupId}")
  ResponseEntity<Void> deleteGroup(@PathVariable UUID groupId);
}
