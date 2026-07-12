package com.cms.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Tenant destek araması başlatma isteği: {@code /app/tenant-rtc/{tenantId}/call}.
 *
 * <p>{@code clientRef}: tenant client'ının ürettiği rastgele kimlik. Caller (tenant) sinyalleri
 * {@code /topic/rtc-client/{clientRef}/rtc}'ye de gönderilir — böylece tenant, userId'yi bilmeye
 * (BFF /me lookup) ihtiyaç duymadan kendi sinyallerine abone olur (prod'da userId zinciri kırılgan).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DtoTenantCallStart {
  private String clientRef;
}
