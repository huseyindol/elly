package com.cms.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DtoRatingIU {
  @NotNull(message = "Post ID is required")
  private Long postId;

  @NotNull(message = "Rating is required")
  @Min(value = 1, message = "Rating must be at least 1")
  @Max(value = 5, message = "Rating must be at most 5")
  private Integer rating;

  private String comment;
}
