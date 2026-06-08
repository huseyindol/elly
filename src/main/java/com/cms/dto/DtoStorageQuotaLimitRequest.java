package com.cms.dto;

import lombok.Data;

/** Tenant kotası ayarlama isteği. limitBytes null → config varsayılanına döner. */
@Data
public class DtoStorageQuotaLimitRequest {
  private Long limitBytes;
}
