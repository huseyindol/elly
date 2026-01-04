package com.cms.dto;

import com.cms.enums.WidgetTypeEnum;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DtoWidgetSummary {
  private Long id;
  private String name;
  private WidgetTypeEnum type;
  private Integer orderIndex;
  private Boolean status;
}
