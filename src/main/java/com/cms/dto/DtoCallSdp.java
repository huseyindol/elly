package com.cms.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** SDP offer/answer relay: {@code /app/rtc/{callId}/sdp}. Backend içeriği parse etmez (opak). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DtoCallSdp {
  /** "offer" | "answer" */
  private String sdpType;
  private String sdp;
}
