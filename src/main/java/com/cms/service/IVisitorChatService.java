package com.cms.service;

import com.cms.dto.DtoChatGroup;
import com.cms.dto.DtoChatMessage;
import com.cms.dto.DtoChatMessageSend;
import com.cms.dto.DtoVisitorIdentity;

import java.util.List;
import java.util.UUID;

/**
 * Tenant Chat (TC) için Z (ziyaretçi) tarafının servisi.
 * Çağıran tarafın TenantContext'i ilgili tenant DB'sine set etmiş olmalı.
 */
public interface IVisitorChatService {

  /**
   * Kayıtlı tenant user için VisitorIdentity'yi getirir; yoksa yaratır (upsert).
   * @param tenantUserId tenant DB users.id
   * @param displayName  isim (genelde username veya ad-soyad)
   * @param email        opsiyonel
   */
  DtoVisitorIdentity ensureForTenantUser(Long tenantUserId, String displayName, String email);

  /** visitor_access=true olan TC group'larını listeler. */
  List<DtoChatGroup> listVisibleGroups();

  /** Group history'sini getirir; yalnızca visitor_access=true ve aynı tenant'taki group. */
  List<DtoChatMessage> getHistory(UUID groupId, UUID before, int limit);

  /** Visitor olarak mesaj yazar. */
  DtoChatMessage sendMessage(UUID groupId, Long visitorId, DtoChatMessageSend payload);
}
