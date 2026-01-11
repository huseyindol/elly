package com.cms.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DtoPageIU {
  private String title;
  private String description;
  private String slug;
  private Boolean status;
  private DtoSeoInfoIU seoInfo;
  private List<Long> componentIds;
}
