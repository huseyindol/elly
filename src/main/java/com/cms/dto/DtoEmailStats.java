package com.cms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Mail+Form v3 (B4) — E-posta gonderim istatistikleri ve son 24 saat FAILED ozeti.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DtoEmailStats {

  private long totalCount;
  private long sentCount;
  private long pendingCount;
  private long failedCount;
  private long quotaExceededCount;
  private long failedLast24hCount;
}
