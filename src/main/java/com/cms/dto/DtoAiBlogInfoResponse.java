package com.cms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DtoAiBlogInfoResponse {
  private String description;
  private String category;
  private String readingTime;
  private String imagePrompt;
}
