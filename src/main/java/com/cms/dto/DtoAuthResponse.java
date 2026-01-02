package com.cms.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DtoAuthResponse {
  private String token;
  private String refreshToken;
  private String type = "Bearer";
  private Long userId;
  private String username;
  private String email;
  private String userCode;
  private Long expiredDate;
}
