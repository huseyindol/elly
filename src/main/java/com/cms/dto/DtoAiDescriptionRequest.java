package com.cms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DtoAiDescriptionRequest {

  @NotBlank(message = "Ad/başlık alanı zorunludur")
  @Size(max = 500, message = "Ad/başlık en fazla 500 karakter olabilir")
  private String name;

  @Size(max = 100, message = "Context en fazla 100 karakter olabilir")
  private String context;
}
