package com.cms.dto;

import java.util.Date;

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
public class DtoPostSummary {
  private Long id;
  private String title;
  private String slug;
  private Boolean status;
  private Integer orderIndex;
  private String description;
  private String category;
  private String coverImage;
  private Date publishedAt;
  private String author;
  private String readingTime;
}
