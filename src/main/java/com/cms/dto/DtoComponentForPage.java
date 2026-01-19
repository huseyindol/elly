package com.cms.dto;

import java.util.List;

import com.cms.enums.ComponentTypeEnum;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Component DTO for use within Page responses.
 * Excludes pageIds and pages to avoid circular references.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DtoComponentForPage {
  private Long id;
  private String name;
  private String description;
  private ComponentTypeEnum type;
  private String content;
  private String template;
  private Integer orderIndex;
  private Boolean status;
  private List<DtoBanner> banners;
  private List<DtoWidget> widgets;
}
