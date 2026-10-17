package com.cms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DtoAiQuotaLimitRequest {
  private Long monthlyTokenLimit;
  private Long dailyRequestLimit;
}
