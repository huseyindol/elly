package com.cms.controller;

import com.cms.dto.DtoChatGroup;
import com.cms.dto.DtoChatGroupAccess;
import com.cms.dto.DtoChatBan;
import com.cms.dto.DtoChatBanRequest;
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

  @Operation(summary = "Mevcut kullanıcının gruptaki okuma/yazma durumu")
  ResponseEntity<RootEntityResponse<DtoChatGroupAccess>> getGroupAccess(UUID groupId);

  @Operation(summary = "Grubu sil")
  ResponseEntity<Void> deleteGroup(UUID groupId);

  @Operation(summary = "TC: guest/visitor banla (yazma engeli — okuma serbest)")
  ResponseEntity<RootEntityResponse<DtoChatBan>> banUser(UUID groupId, DtoChatBanRequest request);

  @Operation(summary = "TC: ban kaldır (sessionId VEYA visitorId)")
  ResponseEntity<Void> unbanUser(UUID groupId, UUID sessionId, Long visitorId);

  @Operation(summary = "TC: gruptaki aktif ban listesi")
  ResponseEntity<RootEntityResponse<List<DtoChatBan>>> listBans(UUID groupId);
}
