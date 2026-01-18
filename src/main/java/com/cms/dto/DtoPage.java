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
public class DtoPage {
  private Long id;
  private String title;
  private String description;
  private String slug;
  private String template;
  private Boolean status;
  private DtoSeoInfo seoInfo;
  private List<DtoComponent> components;
}
