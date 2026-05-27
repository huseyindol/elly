package com.cms.service.impl;

import com.cms.dto.DtoChatGroup;
import com.cms.dto.DtoChatMessage;
import com.cms.dto.DtoChatMessageSend;
import com.cms.dto.DtoVisitorIdentity;
import com.cms.entity.ChatGroup;
import com.cms.entity.ChatMessage;
import com.cms.entity.VisitorIdentity;
import com.cms.exception.ForbiddenException;
import com.cms.exception.ResourceNotFoundException;
import com.cms.mapper.ChatMapper;
import com.cms.repository.ChatGroupRepository;
import com.cms.repository.ChatMessageRepository;
import com.cms.repository.VisitorIdentityRepository;
import com.cms.service.IVisitorChatService;
import com.cms.util.ChatTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VisitorChatService implements IVisitorChatService {

  private final VisitorIdentityRepository visitorRepository;
  private final ChatGroupRepository groupRepository;
  private final ChatMessageRepository messageRepository;
  private final ChatMessageService chatMessageService;
  private final ChatMapper chatMapper;
  private final SimpMessagingTemplate messagingTemplate;

  @Override
  @Transactional
  public DtoVisitorIdentity ensureForTenantUser(Long tenantUserId, String displayName, String email) {
    VisitorIdentity entity = visitorRepository.findByTenantUserId(tenantUserId)
        .orElseGet(() -> {
          VisitorIdentity created = new VisitorIdentity();
          created.setTenantUserId(tenantUserId);
          created.setDisplayName(displayName != null ? displayName : ("user-" + tenantUserId));
          created.setEmail(email);
          return visitorRepository.save(created);
        });
    // Display name / email opsiyonel update
    boolean dirty = false;
    if (displayName != null && !displayName.equals(entity.getDisplayName())) {
      entity.setDisplayName(displayName);
      dirty = true;
    }
    if (email != null && !email.equals(entity.getEmail())) {
      entity.setEmail(email);
      dirty = true;
    }
    entity.setLastSeenAt(new Date());
    if (dirty) {
      entity = visitorRepository.save(entity);
    } else {
      visitorRepository.save(entity);
    }
    return toDto(entity);
  }

  @Override
  @Transactional(readOnly = true)
  public List<DtoChatGroup> listVisibleGroups() {
    // groupRepository.findAll TenantContext'in işaret ettiği DB'den çeker —
    // controller bu service'i çağırmadan önce X-Tenant-Id veya JWT claim ile
    // TenantContext'i set etmiş olmalı.
    return groupRepository.findAll().stream()
        .filter(ChatGroup::isVisitorAccess)
        .map(chatMapper::toGroupDto)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<DtoChatMessage> getHistory(UUID groupId, UUID before, int limit) {
    ChatGroup group = ensureVisitorGroup(groupId);

    Date beforeDate = null;
    if (before != null) {
      beforeDate = messageRepository.findById(before)
          .map(ChatMessage::getCreatedAt)
          .orElse(null);
    }

    int safeLimit = Math.min(Math.max(limit, 1), 100);
    List<ChatMessage> msgs = (beforeDate == null)
        ? messageRepository.findByGroupId(group.getId(), PageRequest.of(0, safeLimit))
        : messageRepository.findByGroupIdBefore(group.getId(), beforeDate, PageRequest.of(0, safeLimit));
    // Reuse ChatMessageService's toDto by going through the public save flow? Hayır,
    // history'de yeni mesaj yaratmayalım. Burada direkt mapper kullanır ve display
    // bilgisini chatMessageService.lookupAdminUsername üzerinden zenginleştiririz.
    return msgs.stream().map(this::toMessageDto).toList();
  }

  @Override
  public DtoChatMessage sendMessage(UUID groupId, Long visitorId, DtoChatMessageSend payload) {
    ChatGroup group = ensureVisitorGroup(groupId);
    if (!visitorRepository.existsById(visitorId)) {
      throw new ResourceNotFoundException("VisitorIdentity not found: " + visitorId);
    }
    DtoChatMessage saved = chatMessageService.saveVisitorMessage(group.getId(), visitorId, payload);
    messagingTemplate.convertAndSend(ChatTopics.messageTopic(group), saved);
    log.debug("Visitor message broadcast: tenant={}, group={}, visitor={}",
        group.getTenantId(), group.getId(), visitorId);
    return saved;
  }

  /**
   * Group'un visitor erişimine açık ve current tenant'a ait olduğunu doğrular.
   * findById TenantContext DB'sinde çalıştığı için tenant izolasyonu otomatik.
   */
  private ChatGroup ensureVisitorGroup(UUID groupId) {
    ChatGroup group = groupRepository.findById(groupId)
        .orElseThrow(() -> new ResourceNotFoundException("Chat group not found: " + groupId));
    if (!group.isVisitorAccess()) {
      throw new ForbiddenException("This group is not open to visitors");
    }
    if (group.getTenantId() == null || group.getTenantId().isBlank()) {
      throw new ForbiddenException("Visitor access requires a tenant chat group");
    }
    return group;
  }

  private DtoVisitorIdentity toDto(VisitorIdentity entity) {
    return DtoVisitorIdentity.builder()
        .id(entity.getId())
        .tenantUserId(entity.getTenantUserId())
        .displayName(entity.getDisplayName())
        .email(entity.getEmail())
        .createdAt(entity.getCreatedAt())
        .lastSeenAt(entity.getLastSeenAt())
        .build();
  }

  private DtoChatMessage toMessageDto(ChatMessage msg) {
    DtoChatMessage dto = chatMapper.toMessageDto(msg);
    if (msg.getSenderType() == com.cms.entity.ChatMessageSenderType.VISITOR && msg.getVisitorId() != null) {
      visitorRepository.findById(msg.getVisitorId())
          .ifPresent(v -> dto.setSenderUsername(v.getDisplayName()));
    } else if (msg.getSenderId() != null) {
      dto.setSenderUsername(chatMessageService.lookupAdminUsername(msg.getSenderId()));
    }
    return dto;
  }
}
