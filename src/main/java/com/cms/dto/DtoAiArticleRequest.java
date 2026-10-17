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
public class DtoAiArticleRequest {

  @NotBlank(message = "Makale konusu zorunludur")
  @Size(min = 3, max = 500, message = "Makale konusu 3 ile 500 karakter arasında olmalıdır")
  private String topic;

  @Size(max = 500, message = "Anahtar kelimeler en fazla 500 karakter olabilir")
  private String keywords;
}
