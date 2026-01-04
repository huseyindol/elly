package com.cms.dto;

import com.cms.enums.ComponentTypeEnum;
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
public class DtoComponentSummary {
  private Long id;
  private String name;
  private Boolean status;
  private ComponentTypeEnum type;
  private Integer orderIndex;
}
