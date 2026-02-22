package com.cms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DtoCmsBasicInfoIU {

  @NotBlank(message = "Section key is required")
  private String sectionKey;

  @NotBlank(message = "Title is required")
  private String title;

  private String description;

  @NotNull(message = "Active status is required")
  private Boolean isActive;

  @NotNull(message = "Sort order is required")
  private Integer sortOrder;
}
