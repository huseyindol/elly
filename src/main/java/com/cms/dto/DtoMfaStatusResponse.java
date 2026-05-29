package com.cms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Mevcut kullanıcının 2FA durumu.
 * GET /api/v1/auth/mfa/status — panel "2FA açık/kapalı" durumunu bununla gösterir.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DtoMfaStatusResponse {
  /** true ise kullanıcının 2FA'sı etkin ve setup'ı doğrulanmış. */
  private boolean mfaEnabled;
}
