package com.cms.dto;

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
  private Long postId;
  // private Long articleId;
  private Long parentCommentId;
  private Boolean status;
  private DtoComment parentComment;
}
