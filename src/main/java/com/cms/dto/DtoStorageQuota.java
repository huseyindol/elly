package com.cms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Tenant depolama kullanımı yanıtı (panel kullanım çubuğu). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DtoStorageQuota {
  private String tenantId;
  private long usedBytes;
  private long limitBytes;     // etkin limit (override yoksa config varsayılanı)
  private double usedPercent;  // 0-100 (1 ondalık)
}
