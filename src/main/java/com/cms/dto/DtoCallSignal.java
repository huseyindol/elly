package com.cms.dto;

import java.util.UUID;

import com.cms.enums.CallSignalType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Server → client tek sinyal zarfı ({@code /user/queue/rtc}). Tipe göre alanlar kısmen dolar:
 * INCOMING_CALL → callId + peer + ringAll; SDP → sdpType/sdp; ICE → candidate...; ERROR → reason.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DtoCallSignal {
  private CallSignalType type;
  private UUID callId;

  /** Karşı taraf (INCOMING_CALL/ANSWERED). Ring-all INCOMING_CALL'da arayan tenant kullanıcısı. */
  private Long peerUserId;
  private String peerDisplayName;

  /** true → tenant destek araması (ring-all); UI "gelen destek çağrısı" gösterir. */
  private boolean ringAll;

  // SDP relay
  private String sdpType;
  private String sdp;

  // ICE relay
  private String candidate;
  private String sdpMid;
  private Integer sdpMLineIndex;

  /** ERROR/UNAVAILABLE/BUSY için açıklama. */
  private String reason;
}
