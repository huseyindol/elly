package com.cms.mapper;

import com.cms.dto.DtoChatGroup;
import com.cms.dto.DtoChatMessage;
import com.cms.entity.ChatGroup;
import com.cms.entity.ChatMessage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ChatMapper {

  DtoChatGroup toGroupDto(ChatGroup group);

  @Mapping(target = "deleted", expression = "java(message.getDeletedAt() != null)")
  @Mapping(target = "senderUsername", ignore = true) // ChatMessageService.toDto() tarafından doldurulur
  @Mapping(target = "senderDisplayName", source = "senderDisplayName")
  DtoChatMessage toMessageDto(ChatMessage message);
}
