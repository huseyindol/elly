package com.cms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/** Mevcut kullanıcının bir gruptaki okuma/yazma durumu. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DtoChatGroupAccess {

  private UUID groupId;
  private boolean member;
  private boolean canRead;
  private boolean canWrite;

  /** canWrite=false ise panel banner / composer disable nedeni */
  private String denialMessage;

  /** {@code CHAT_WRITE_FORBIDDEN} vb. */
  private String denialCode;
}
