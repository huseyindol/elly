package com.cms.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class DtoGuestTokenResponse {
  private String token;
  private Long expiresIn; // saniye cinsinden
  private String displayName;
}
