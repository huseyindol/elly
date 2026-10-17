package com.cms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DtoAiQuota {
  private String tenantId;
  private long monthlyTokenLimit;
  private long monthlyTokensUsed;
  private long monthlyTokensRemaining;
  private double monthlyTokenUsedPercent;
  private long dailyRequestLimit;
  private long dailyRequestsUsed;
  private long dailyRequestsRemaining;
  private double dailyRequestUsedPercent;
  private long dailyTokensUsed;
  private long monthlyInputTokens;
  private long monthlyOutputTokens;
  private long monthlyThoughtTokens;
  private long monthlyCachedTokens;
  private double monthlyEstimatedCostUsd;
  private double totalEstimatedCostUsd;
}
