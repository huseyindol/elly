package com.cms.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DtoComment {
  private Long id;
  private String name;
  private String email;
  private String content;
  private DtoPost post;
  // private DtoArticle article;
  private DtoComment parentComment;
  private List<DtoComment> subComments = new ArrayList<>();
  private Boolean status;
}
