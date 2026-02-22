package com.cms.dto;

import java.util.Map;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DtoCmsContentIU {

  private UUID basicInfoId;

  @Valid
  private DtoCmsBasicInfoIU basicInfo;

  @NotBlank(message = "Content type is required")
  private String contentType;

  private Map<String, Object> metadata;
}
