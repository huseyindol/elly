package com.cms.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DtoPostIU {
  private String title;
  private String content;
  private String slug;
  private Boolean status;
  private Integer orderIndex;
  private DtoSeoInfoIU seoInfo;
}
