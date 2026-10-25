package com.cms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DtoPushDeviceRequest {

  @NotBlank(message = "Cihaz token zorunludur")
  @Size(max = 512)
  private String token;

  @Size(max = 32)
  private String platform;

  @Size(max = 512)
  private String voipToken;

  @Size(max = 120)
  private String deviceName;
}
