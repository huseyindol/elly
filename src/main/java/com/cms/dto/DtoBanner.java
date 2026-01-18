package com.cms.dto;

import com.cms.entity.BannerImage;

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
  private BannerImage images;
  private String link;
  private String target;
  private String type;
  private Integer orderIndex;
  private Boolean status;
}
