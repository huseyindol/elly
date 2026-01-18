package com.cms.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DtoBannerSummary {
  private Long id;
  private String title;
  private Integer orderIndex;
  private Boolean status;
  private String subFolder;
}
