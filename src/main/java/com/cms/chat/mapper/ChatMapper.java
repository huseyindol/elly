package com.cms.chat.mapper;

import com.cms.chat.dto.DtoChatGroup;
import com.cms.chat.dto.DtoChatMessage;
import com.cms.chat.entity.ChatGroup;
import com.cms.chat.entity.ChatMessage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ChatMapper {

  DtoChatGroup toGroupDto(ChatGroup group);

  @Mapping(target = "deleted", expression = "java(message.getDeletedAt() != null)")
  @Mapping(target = "senderUsername", ignore = true)
  DtoChatMessage toMessageDto(ChatMessage message);
}
