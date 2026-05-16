package com.cms.chat.service.impl;

import com.cms.chat.dto.DtoChatMessage;
import com.cms.chat.dto.DtoChatMessageSend;
import com.cms.chat.entity.ChatMessage;
import com.cms.chat.entity.ChatMessageEdit;
import com.cms.chat.mapper.ChatMapper;
import com.cms.chat.repository.ChatMessageEditRepository;
import com.cms.chat.repository.ChatMessageReadRepository;
import com.cms.chat.repository.ChatMessageRepository;
import com.cms.chat.service.IChatMessageService;
import com.cms.exception.ForbiddenException;
import com.cms.exception.ResourceNotFoundException;
import com.cms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
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
  private final ChatGroupService groupService;
  private final UserRepository userRepository;
  private final ChatMapper chatMapper;

  @Value("${chat.message.max-length:4000}")
  private int maxMessageLength;

  @Override
  @Transactional
  public DtoChatMessage saveMessage(UUID groupId, Long senderId, DtoChatMessageSend dto) {
    groupService.checkAccess(groupId, senderId);

    String sanitized = Jsoup.clean(dto.getContent(), Safelist.none());
    if (sanitized.length() > maxMessageLength) {
      throw new com.cms.exception.BadRequestException(
          "Message exceeds max length of " + maxMessageLength + " characters");
    }

    ChatMessage msg = new ChatMessage();
    msg.setGroupId(groupId);
    msg.setSenderId(senderId);
    msg.setContent(sanitized);
    msg.setContentType(dto.getContentType() != null ? dto.getContentType() : com.cms.chat.entity.ChatMessageType.TEXT);
    msg.setFileUrl(dto.getFileUrl());
    msg.setParentId(dto.getParentId());
    msg = messageRepository.save(msg);

    return toDto(msg);
  }

  @Override
  public List<DtoChatMessage> getHistory(UUID groupId, Long requesterId, UUID before, int limit) {
    groupService.checkAccess(groupId, requesterId);

    Date beforeDate = null;
    if (before != null) {
      beforeDate = messageRepository.findById(before)
          .map(ChatMessage::getCreatedAt)
          .orElse(null);
    }

    return messageRepository.findByGroupIdCursor(groupId, beforeDate, Math.min(limit, 100))
        .stream()
        .map(this::toDto)
        .toList();
  }

  @Override
  @Transactional
  public DtoChatMessage editMessage(UUID messageId, Long requesterId, String newContent) {
    ChatMessage msg = findMessageOrThrow(messageId);
    if (!msg.getSenderId().equals(requesterId)) {
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
    if (!msg.getSenderId().equals(requesterId) && !groupService.isMember(msg.getGroupId(), requesterId)) {
      throw new ForbiddenException("Cannot delete this message");
    }
    msg.setDeletedAt(new Date());
    msg.setContent("[deleted]");
    messageRepository.save(msg);
  }

  @Override
  @Transactional
  public void markGroupAsRead(UUID groupId, Long userId) {
    groupService.checkAccess(groupId, userId);
    readRepository.markAllAsRead(groupId, userId);
  }

  private ChatMessage findMessageOrThrow(UUID messageId) {
    return messageRepository.findById(messageId)
        .orElseThrow(() -> new ResourceNotFoundException("Message not found: " + messageId));
  }

  private DtoChatMessage toDto(ChatMessage msg) {
    DtoChatMessage dto = chatMapper.toMessageDto(msg);
    userRepository.findById(msg.getSenderId())
        .ifPresent(u -> dto.setSenderUsername(u.getUsername()));
    return dto;
  }
}
