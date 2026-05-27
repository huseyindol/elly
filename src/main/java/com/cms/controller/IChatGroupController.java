package com.cms.controller;

import com.cms.dto.DtoChatGroup;
import com.cms.dto.DtoChatGroupCreate;
import com.cms.dto.DtoChatMember;
import com.cms.entity.RootEntityResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

@Tag(name = "Chat Groups", description = "Admin panel chat grup yönetimi")
public interface IChatGroupController {

  @Operation(summary = "Yeni grup oluştur")
  ResponseEntity<RootEntityResponse<DtoChatGroup>> createGroup(DtoChatGroupCreate dto);

  @Operation(summary = "Katıldığım grupları listele")
  ResponseEntity<RootEntityResponse<List<DtoChatGroup>>> getMyGroups();

  @Operation(summary = "Grup detayı")
  ResponseEntity<RootEntityResponse<DtoChatGroup>> getGroup(UUID groupId);

  @Operation(summary = "DM getir veya oluştur")
  ResponseEntity<RootEntityResponse<DtoChatGroup>> getOrCreateDm(Long targetUserId);

  @Operation(summary = "Gruba üye ekle")
  ResponseEntity<RootEntityResponse<DtoChatMember>> addMember(UUID groupId, Long userId);

  @Operation(summary = "Gruptan üye çıkar")
  ResponseEntity<Void> removeMember(UUID groupId, Long userId);

  @Operation(summary = "Grup üyelerini listele")
  ResponseEntity<RootEntityResponse<List<DtoChatMember>>> getMembers(UUID groupId);

  @Operation(summary = "Grubu sil")
  ResponseEntity<Void> deleteGroup(UUID groupId);
}
