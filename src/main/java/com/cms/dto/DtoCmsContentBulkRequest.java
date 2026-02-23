package com.cms.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DtoCmsContentBulkRequest {

  private UUID basicInfoId;

  @Valid
  private DtoCmsBasicInfoIU basicInfo;

  @NotEmpty(message = "Contents cannot be empty")
  @Valid
  private List<ContentItem> contents;

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ContentItem {
    @NotBlank(message = "Content type is required")
    private String contentType;

    private Map<String, Object> metadata;
  }
}
