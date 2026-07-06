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
import com.cms.enums.NotificationType;
import com.cms.service.IChatBanService;
import com.cms.service.IChatMessageService;
import com.cms.service.INotificationService;
import com.cms.exception.ForbiddenException;
import com.cms.exception.ResourceNotFoundException;
import com.cms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.beans.factory.ObjectProvider;
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
@Slf4j
public class ChatMessageService implements IChatMessageService {

  private final ChatMessageRepository messageRepository;
  private final ChatMessageReadRepository readRepository;
  private final ChatMessageEditRepository editRepository;
  private final VisitorIdentityRepository visitorIdentityRepository;
  private final ChatGroupService groupService;
  private final UserRepository userRepository;
  private final ChatMapper chatMapper;
  /**
   * Kendi proxy'si — lookupAdminUsername'in {@code REQUIRES_NEW}'i self-invocation'da
   * etkisiz kalmasın diye Spring proxy üzerinden çağrılır. Aksi halde OSIV'in tenant DB'ye
   * pinlediği bağlantı kullanılır ve admin username yanlış DB'den (tenant) okunur.
   */
  private final ObjectProvider<ChatMessageService> selfProvider;
  private final IChatBanService chatBanService;
  private final INotificationService notificationService;

  @Value("${chat.message.max-length:4000}")
  private int maxMessageLength;

  @Override
  @Transactional
  public DtoChatMessage saveMessage(UUID groupId, Long senderId, String senderUsername, DtoChatMessageSend dto) {
    groupService.checkWriteAccess(groupId, senderId);

    ChatMessage msg = buildBaseMessage(groupId, dto);
    msg.setSenderType(ChatMessageSenderType.ADMIN);
    msg.setSenderId(senderId);
    msg.setVisitorId(null);
    // Admin gönderen adı YAZARKEN (bellekteki principal'dan) denormalize edilir → okumada
    // cross-DB lookup (OSIV ile tenant'a pinlenir, yanlış DB'den okur) gerekmez.
    msg.setSenderDisplayName(senderUsername);
    msg = messageRepository.save(msg);

    // TENANT rollü (tenant-site login'li, WS üzerinden yazan) gönderici → adminlere bildirim.
    // Panel rollerinin (SUPER_ADMIN/ADMIN/EDITOR/VIEWER) mesajları bildirim üretmez (spam).
    try {
      boolean tenantSender = userRepository.findById(senderId)
          .map(u -> u.getRoles().stream().anyMatch(r -> "TENANT".equals(r.getName())))
          .orElse(false);
      if (tenantSender) {
        notifyAdminsOfTenantMessage(groupId, senderUsername, msg.getContent());
      }
    } catch (Exception e) {
      log.warn("Chat bildirim kontrolü başarısız (mesaj kaydedildi): {}", e.getMessage());
    }

    return toDto(msg);
  }

  /**
   * Visitor (Z) tarafından gelen mesaj — sender_type=VISITOR olarak kaydedilir.
   * Çağıran tarafın TenantContext'i ilgili tenant DB'sine set etmiş olması beklenir
   * (PublicApiFilter ya da TenantChatPublicController yapar).
   */
  @Transactional
  public DtoChatMessage saveVisitorMessage(UUID groupId, Long visitorId, DtoChatMessageSend dto) {
    VisitorIdentity visitor = visitorIdentityRepository.findById(visitorId)
        .orElseThrow(() -> new ResourceNotFoundException("VisitorIdentity not found: " + visitorId));
    if (chatBanService.isVisitorBanned(groupId, visitorId)) {
      throw new ForbiddenException("Bu sohbette yazma yetkiniz kaldırıldı", "CHAT_BANNED");
    }

    ChatMessage msg = buildBaseMessage(groupId, dto);
    msg.setSenderType(ChatMessageSenderType.VISITOR);
    msg.setSenderId(null);
    msg.setVisitorId(visitorId);
    msg = messageRepository.save(msg);

    // Tenant-site ziyaretçisinden gelen mesaj → adminlere bildirim
    notifyAdminsOfTenantMessage(groupId, visitor.getDisplayName(), msg.getContent());

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
    UUID sid = UUID.fromString(sessionId);
    if (chatBanService.isGuestBanned(groupId, sid)) {
      throw new ForbiddenException("Bu sohbette yazma yetkiniz kaldırıldı", "CHAT_BANNED");
    }

    ChatMessage msg = buildBaseMessage(groupId, dto);
    msg.setSenderType(ChatMessageSenderType.GUEST);
    msg.setSenderId(null);
    msg.setVisitorId(null);
    msg.setSessionId(sid);
    msg.setSenderDisplayName(displayName);
    msg = messageRepository.save(msg);

    // Anonim guest'ten gelen mesaj → adminlere bildirim
    notifyAdminsOfTenantMessage(groupId, displayName, msg.getContent());

    return toDto(msg);
  }

  /**
   * Tenant tarafından (visitor/guest/TENANT rollü kullanıcı) gelen chat mesajında
   * SUPER_ADMIN + ADMIN kullanıcılarına bildirim üretir: DB kaydı + WS push
   * (/user/queue/notifications) → panel header rozeti + tarayıcı bildirimi.
   * Fire-and-forget: bildirim hatası mesaj akışını ASLA bozmaz.
   */
  private void notifyAdminsOfTenantMessage(UUID groupId, String senderName, String content) {
    try {
      String who = (senderName != null && !senderName.isBlank()) ? senderName : "Ziyaretçi";
      String preview = content == null ? ""
          : (content.length() > 80 ? content.substring(0, 80) + "…" : content);
      java.util.Map<String, Object> metadata = new java.util.HashMap<>();
      metadata.put("groupId", groupId.toString());
      notificationService.notifySuperAdminAndAdminUsers(
          NotificationType.CHAT_MESSAGE,
          "Yeni chat mesajı",
          who + ": " + preview,
          "/chat",
          TenantContext.getTenantId(),
          metadata);
    } catch (Exception ex) {
      log.warn("Chat mesaj bildirimi gönderilemedi: group={} hata={}", groupId, ex.getMessage());
    }
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
    // Sorgu createdAt DESC (en yeni N / cursor'dan onceki N'i almak icin dogru).
    // Goruntu ASC olmali (eskiden yeniye, ustten alta) -> sayfayi ters cevir.
    List<ChatMessage> msgs = new java.util.ArrayList<>((beforeDate == null)
        ? messageRepository.findByGroupId(groupId, PageRequest.of(0, safeLimit))
        : messageRepository.findByGroupIdBefore(groupId, beforeDate, PageRequest.of(0, safeLimit)));
    java.util.Collections.reverse(msgs);
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
      // Admin sender: yazarken denormalize edilen isim (DB lookup yok → OSIV sorunu yok).
      // Eski mesajlarda (sender_display_name boş) basedb lookup'a fallback.
      String adminUsername = msg.getSenderDisplayName() != null
          ? msg.getSenderDisplayName()
          : selfProvider.getObject().lookupAdminUsername(msg.getSenderId());
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
    // Mevcut tenant context'inde — kullanıcı kendi tenant DB'sinde (basedb yok).
    return userRepository.findById(userId)
        .map(u -> u.getUsername())
        .orElse(null);
  }
}
