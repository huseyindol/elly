package com.cms.dto;

import java.util.Date;

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
public class DtoAiUsageLog {

  private Long id;
  private Date createdAt;
  private String requestId;
  private Integer stepOrder;
  private String endpoint;
  private String action;
  private String model;
  private String username;
  private String tenantId;
  private String userInput;
  private String promptText;
  private String responseText;
  private String parsedOutput;
  private Integer promptChars;
  private Integer responseChars;
  private Integer inputTokens;
  private Integer outputTokens;
  private Integer thoughtTokens;
  private Integer cachedTokens;
  private Integer totalTokens;
  private Long durationMs;
  private String status;
  private Integer httpStatus;
  private String errorCode;
  private String errorMessage;
}
