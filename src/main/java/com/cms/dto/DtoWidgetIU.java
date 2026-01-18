package com.cms.dto;

import java.util.List;

import com.cms.enums.WidgetTypeEnum;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DtoWidgetIU {
  private String name;
  private String description;
  private WidgetTypeEnum type;
  private String content;
  private String template;
  private Integer orderIndex;
  private Boolean status;
  private List<Long> bannerIds;
  private List<Long> postIds;
}
