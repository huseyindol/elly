package com.cms.dto;

import com.cms.entity.ChatMessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class DtoChatMessageSend {

  @NotBlank
  @Size(max = 4000)
  private String content;

  private ChatMessageType contentType = ChatMessageType.TEXT;

  // NM4: Yalnızca sunucumuzun kendi upload path'lerine izin ver
  // saveFile("chat") → "assets/chat/..." döndürür
  @Pattern(
      regexp = "^assets/[\\w\\-./]+$",
      message = "fileUrl only accepts internal upload paths (assets/...)"
  )
  private String fileUrl;

  private UUID parentId;
}
