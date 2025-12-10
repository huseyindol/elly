package com.cms.dto;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DtoRating {
  private Long id;
  private DtoPost post;
  private Integer rating;
  private String comment;
  private Date createdAt;
  private Date updatedAt;
}
