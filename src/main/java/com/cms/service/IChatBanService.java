package com.cms.service;

import com.cms.dto.DtoChatBan;
import com.cms.dto.DtoChatBanRequest;

import java.util.List;
import java.util.UUID;

/** TC ban/unban + gönderim enforcement sorguları. */
public interface IChatBanService {

  /** Bir guest (sessionId) veya visitor (visitorId) bu grupta yazamaz yapılır. Idempotent. */
  DtoChatBan ban(UUID groupId, DtoChatBanRequest request, Long requesterId, String requesterUsername);

  /** Ban kaldırılır. Idempotent (kayıt yoksa no-op). */
  void unban(UUID groupId, UUID sessionId, Long visitorId, Long requesterId);

  /** Gruptaki aktif ban kayıtları. */
  List<DtoChatBan> listBans(UUID groupId);

  /** Enforcement: guest bu grupta banlı mı? */
  boolean isGuestBanned(UUID groupId, UUID sessionId);

  /** Enforcement: kayıtlı visitor bu grupta banlı mı? */
  boolean isVisitorBanned(UUID groupId, Long visitorId);
}
