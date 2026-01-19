package com.cms.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Page DTO for detailed view (e.g., getPageBySlug).
 * Uses DtoComponentForPage which excludes pageIds/pages.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DtoPageDetail {
  private Long id;
  private String title;
  private String description;
  private String slug;
  private String template;
  private Boolean status;
  private DtoSeoInfo seoInfo;
  private List<DtoComponentForPage> components;
}
