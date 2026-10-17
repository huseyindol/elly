package com.cms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DtoAiSeoResponse {
  private String seoTitle;
  private String seoDescription;
  private String seoKeywords;
}
