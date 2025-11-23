package com.cms.dto;

import java.util.List;

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
public class DtoComponent {
  private Long id;
  private String name;
  private String description;
  private ComponentTypeEnum type;
  private String content;
  private Integer orderIndex;
  private Boolean status;
  private List<Long> pageIds;
  private List<DtoBanner> banners;
}
