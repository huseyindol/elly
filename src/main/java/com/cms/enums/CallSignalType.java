package com.cms.enums;

/**
 * WebRTC sinyalleşme mesaj tipleri — esas yol {@code /topic/user/{userId}/rtc}
 * ({@code /user/queue/rtc} yedek; tenant caller ayrıca {@code /topic/rtc-client/{clientRef}/rtc})
 * üzerinden client'a gider ({@code com.cms.dto.DtoCallSignal.type}).
 */
public enum CallSignalType {
  /** Gelen çağrı (callee'ye / ring-all'da tüm panel kullanıcılarına). */
  INCOMING_CALL,
  /** Arayan tarafa "çalıyor" onayı. */
  RINGING,
  /** Çağrı cevaplandı (iki tarafa) — SDP offer/answer başlar. */
  ANSWERED,
  /** Ring-all'da başka biri cevapladı → diğerlerinin zili kapanır. */
  CALL_TAKEN,
  REJECTED,
  CANCELLED,
  HANGUP,
  /** Cevapsız timeout. */
  TIMEOUT,
  /** Callee offline / ring-all'da kimse online değil. */
  UNAVAILABLE,
  /** Callee aktif başka bir çağrıda. */
  BUSY,
  /** Hata (hiyerarşi reddi, geçersiz callId, yön ihlali...). */
  ERROR,
  /** SDP offer/answer relay (opak). */
  SDP,
  /** ICE candidate relay (opak). */
  ICE
}
