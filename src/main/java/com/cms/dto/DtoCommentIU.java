package com.cms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DtoCommentIU {
  private String name;
  @Email(message = "Email is not valid")
  @NotBlank(message = "Email is required")
  private String email;
  private String content;
  private Long postId;
  // private Long articleId;
  private Long parentCommentId;
  private Boolean status;
}
