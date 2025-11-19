package com.cms.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DtoSeoInfo {
  private Long id;
  private String title;
  private String description;
  private String keywords;
  private String canonicalUrl;
  private Boolean noIndex;
  private Boolean noFollow;
}
