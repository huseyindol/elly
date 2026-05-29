package com.cms.service;

import com.cms.dto.DtoChatMessage;
import com.cms.dto.DtoChatMessageSend;

import java.util.List;
import java.util.UUID;

public interface IChatMessageService {

  DtoChatMessage saveMessage(UUID groupId, Long senderId, DtoChatMessageSend dto);

  /** Kayıtlı tenant user (VISITOR) mesajı. */
  DtoChatMessage saveVisitorMessage(UUID groupId, Long visitorId, DtoChatMessageSend dto);

  /** Anonim guest (GUEST) mesajı — session_id + displayName ile. */
  DtoChatMessage saveGuestMessage(UUID groupId, String sessionId, String displayName, DtoChatMessageSend dto);

  List<DtoChatMessage> getHistory(UUID groupId, Long requesterId, UUID before, int limit);

  DtoChatMessage editMessage(UUID messageId, Long requesterId, String newContent);

  void deleteMessage(UUID messageId, Long requesterId);

  void markGroupAsRead(UUID groupId, Long userId);
}
