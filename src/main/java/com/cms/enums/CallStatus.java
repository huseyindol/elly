package com.cms.enums;

/**
 * WebRTC çağrı yaşam döngüsü durumu (call_logs.status).
 * Durum makinesi: RINGING → {ANSWERED, REJECTED, CANCELLED, MISSED}; ANSWERED → COMPLETED.
 */
public enum CallStatus {
  RINGING,
  ANSWERED,
  COMPLETED,
  MISSED,
  REJECTED,
  CANCELLED
}
