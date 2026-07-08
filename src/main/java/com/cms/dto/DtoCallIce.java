package com.cms.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** ICE candidate relay: {@code /app/rtc/{callId}/ice}. Backend içeriği parse etmez (opak). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DtoCallIce {
  private String candidate;
  private String sdpMid;
  private Integer sdpMLineIndex;
}
