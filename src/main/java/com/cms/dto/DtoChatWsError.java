package com.cms.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/** WebSocket {@code /user/queue/chat-errors} kuyruğuna giden hata payload'ı. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DtoChatWsError {

  private String errorCode;
  private String message;
  private UUID groupId;
}
