package com.cms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DtoPushDeviceResponse {

  private Long id;
  private Long userId;
  private String token;
  private String platform;
  private String voipToken;
  private String deviceName;
}
