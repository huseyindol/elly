package com.cms.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DtoBanner {
  private Long id;
  private String title;
  private String altText;
  private String image;
  private String link;
  private String target;
  private String type;
  private Integer orderIndex;
  private Boolean status;
}
