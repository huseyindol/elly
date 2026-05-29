package com.cms.service.impl;

import com.cms.config.TenantContext;
import com.cms.dto.DtoChatMessage;
import com.cms.dto.DtoChatMessageSend;
import com.cms.entity.ChatMessage;
import com.cms.entity.ChatMessageEdit;
import com.cms.entity.ChatMessageSenderType;
import com.cms.entity.ChatMessageType;
import com.cms.entity.VisitorIdentity;
import com.cms.mapper.ChatMapper;
import com.cms.repository.ChatMessageEditRepository;
import com.cms.repository.ChatMessageReadRepository;
import com.cms.repository.ChatMessageRepository;
import com.cms.repository.VisitorIdentityRepository;
import com.cms.service.IChatMessageService;
import com.cms.exception.ForbiddenException;
import com.cms.exception.ResourceNotFoundException;
import com.cms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatMessageService implements IChatMessageService {

  private final ChatMessageRepository messageRepository;
  private final ChatMessageReadRepository readRepository;
  private final ChatMessageEditRepository editRepository;
  private final VisitorIdentityRepository visitorIdentityRepository;
  private final ChatGroupService groupService;
  private final UserRepository userRepository;
  private final ChatMapper chatMapper;

  @Value("${chat.message.max-length:4000}")
  private int maxMessageLength;

  @Value("${app.tenants.default-tenant:basedb}")
  private String defaultTenant;

  @Override
  @Transactional
  public DtoChatMessage saveMessage(UUID groupId, Long senderId, DtoChatMessageSend dto) {
    groupService.checkWriteAccess(groupId, senderId);

    ChatMessage msg = buildBaseMessage(groupId, dto);
    msg.setSenderType(ChatMessageSenderType.ADMIN);
    msg.setSenderId(senderId);
    msg.setVisitorId(null);
    msg = messageRepository.save(msg);

    return toDto(msg);
  }

  /**
   * Visitor (Z) tarafından gelen mesaj — sender_type=VISITOR olarak kaydedilir.
   * Çağıran tarafın TenantContext'i ilgili tenant DB'sine set etmiş olması beklenir
   * (PublicApiFilter ya da TenantChatPublicController yapar).
   */
  @Transactional
  public DtoChatMessage saveVisitorMessage(UUID groupId, Long visitorId, DtoChatMessageSend dto) {
    if (!visitorIdentityRepository.existsById(visitorId)) {
      throw new ResourceNotFoundException("VisitorIdentity not found: " + visitorId);
    }

    ChatMessage msg = buildBaseMessage(groupId, dto);
    msg.setSenderType(ChatMessageSenderType.VISITOR);
    msg.setSenderId(null);
    msg.setVisitorId(visitorId);
    msg = messageRepository.save(msg);

    return toDto(msg);
  }

  /**
   * Anonim guest (website ziyaretçisi) mesajı — sender_type=GUEST.
   * session_id + sender_display_name doldurulur; sender_id/visitor_id null.
   * Erişim kontrolü {@code visitorAccess=true} flag'i üzerinden yapılır.
   * Çağıran tarafın TenantContext'i ilgili tenant DB'sine set etmiş olması beklenir.
   */
  @Transactional
  public DtoChatMessage saveGuestMessage(UUID groupId, String sessionId, String displayName,
      DtoChatMessageSend dto) {
    groupService.checkGuestWriteAccess(groupId);

    ChatMessage msg = buildBaseMessage(groupId, dto);
    msg.setSenderType(ChatMessageSenderType.GUEST);
    msg.setSenderId(null);
    msg.setVisitorId(null);
    msg.setSessionId(UUID.fromString(sessionId));
    msg.setSenderDisplayName(displayName);
    msg = messageRepository.save(msg);

    return toDto(msg);
  }

  private ChatMessage buildBaseMessage(UUID groupId, DtoChatMessageSend dto) {
    String sanitized = Jsoup.clean(dto.getContent(), Safelist.none());
    if (sanitized.length() > maxMessageLength) {
      throw new com.cms.exception.BadRequestException(
          "Message exceeds max length of " + maxMessageLength + " characters");
    }

    ChatMessage msg = new ChatMessage();
    msg.setGroupId(groupId);
    msg.setContent(sanitized);
    msg.setContentType(dto.getContentType() != null ? dto.getContentType() : ChatMessageType.TEXT);
    msg.setFileUrl(dto.getFileUrl());
    msg.setParentId(dto.getParentId());
    return msg;
  }

  @Override
  public List<DtoChatMessage> getHistory(UUID groupId, Long requesterId, UUID before, int limit) {
    groupService.checkReadAccess(groupId, requesterId);

    Date beforeDate = null;
    if (before != null) {
      beforeDate = messageRepository.findById(before)
          .map(ChatMessage::getCreatedAt)
          .orElse(null);
    }

    int safeLimit = Math.min(limit, 100);
    List<ChatMessage> msgs = (beforeDate == null)
        ? messageRepository.findByGroupId(groupId, PageRequest.of(0, safeLimit))
        : messageRepository.findByGroupIdBefore(groupId, beforeDate, PageRequest.of(0, safeLimit));
    return msgs
        .stream()
        .map(this::toDto)
        .toList();
  }

  @Override
  @Transactional
  public DtoChatMessage editMessage(UUID messageId, Long requesterId, String newContent) {
    ChatMessage msg = findMessageOrThrow(messageId);

    // Visitor mesajları edit edilemez (MVP) — sadece kendi admin mesajı edit edilebilir
    if (msg.getSenderType() == ChatMessageSenderType.VISITOR) {
      throw new ForbiddenException("Visitor messages are not editable");
    }
    if (!requesterId.equals(msg.getSenderId())) {
      throw new ForbiddenException("You can only edit your own messages");
    }
    if (msg.getDeletedAt() != null) {
      throw new com.cms.exception.BadRequestException("Cannot edit a deleted message");
    }

    // Edit geçmişine kaydet
    ChatMessageEdit edit = new ChatMessageEdit();
    edit.setMessageId(messageId);
    edit.setPreviousContent(msg.getContent());
    editRepository.save(edit);

    String sanitized = Jsoup.clean(newContent, Safelist.none());
    msg.setContent(sanitized);
    msg.setEditedAt(new Date());
    msg = messageRepository.save(msg);

    return toDto(msg);
  }

  @Override
  @Transactional
  public void deleteMessage(UUID messageId, Long requesterId) {
    ChatMessage msg = findMessageOrThrow(messageId);
    boolean isOwnAdminMessage = msg.getSenderType() == ChatMessageSenderType.ADMIN
        && requesterId.equals(msg.getSenderId());
    if (!isOwnAdminMessage && !groupService.isMember(msg.getGroupId(), requesterId)) {
      throw new ForbiddenException("Cannot delete this message");
    }
    msg.setDeletedAt(new Date());
    msg.setContent("[deleted]");
    messageRepository.save(msg);
  }

  @Override
  @Transactional
  public void markGroupAsRead(UUID groupId, Long userId) {
    groupService.checkReadAccess(groupId, userId);
    readRepository.markAllAsRead(groupId, userId);
  }

  private ChatMessage findMessageOrThrow(UUID messageId) {
    return messageRepository.findById(messageId)
        .orElseThrow(() -> new ResourceNotFoundException("Message not found: " + messageId));
  }

  /**
   * DTO'ya çevirir + sender display name'i zenginleştirir.
   * <ul>
   *   <li>ADMIN sender → username basedb'den çekilir (gerekirse tenant context geçici switch)</li>
   *   <li>VISITOR sender → display_name aynı tenant DB visitor_identities'ten çekilir</li>
   * </ul>
   */
  private DtoChatMessage toDto(ChatMessage msg) {
    DtoChatMessage dto = chatMapper.toMessageDto(msg);
    if (msg.getSenderType() == ChatMessageSenderType.GUEST) {
      // Guest — isim mesaja denormalize edilmiş, DB lookup gerekmez
      dto.setSenderUsername(msg.getSenderDisplayName());
    } else if (msg.getSenderType() == ChatMessageSenderType.VISITOR && msg.getVisitorId() != null) {
      visitorIdentityRepository.findById(msg.getVisitorId())
          .ifPresent(v -> dto.setSenderUsername(v.getDisplayName()));
    } else if (msg.getSenderId() != null) {
      // Admin sender — basedb'den çek (TC group'unda TenantContext tenant'a set olabilir)
      String adminUsername = lookupAdminUsername(msg.getSenderId());
      dto.setSenderUsername(adminUsername);
    }
    return dto;
  }

  /**
   * Admin user'ının username'ini basedb'den okur. TC akışında TenantContext tenant'a
   * set olduğu için query basedb'ye yönlendirilmek üzere geçici switch yapılır.
   *
   * <p>{@code public} olarak işaretlendi çünkü VisitorChatService gibi başka servisler
   * Spring proxy üzerinden bu method'u çağırabilsin ({@code REQUIRES_NEW} davranışı
   * için).</p>
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  public String lookupAdminUsername(Long userId) {
    String originalTenant = TenantContext.getTenantId();
    try {
      TenantContext.setTenantId(defaultTenant);
      return userRepository.findById(userId)
          .map(u -> u.getUsername())
          .orElse(null);
    } finally {
      TenantContext.setTenantId(originalTenant);
    }
  }
}
