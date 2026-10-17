package com.cms.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DtoAiUsageStats {

  private long totalCalls;
  private long successCalls;
  private long errorCalls;
  private long totalInputTokens;
  private long totalOutputTokens;
  private long totalThoughtTokens;
  private long totalCachedTokens;
  private long totalTokens;
  private double avgDurationMs;
  private List<DtoAiActionStat> byAction;

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class DtoAiActionStat {
    private String action;
    private String endpoint;
    private long callCount;
    private long successCount;
    private long errorCount;
    private long inputTokens;
    private long outputTokens;
    private long thoughtTokens;
    private long totalTokens;
    private double avgDurationMs;
  }
}
