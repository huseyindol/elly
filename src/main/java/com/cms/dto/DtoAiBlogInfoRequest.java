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
public class DtoAiBlogInfoRequest {

  @NotBlank(message = "Başlık alanı zorunludur")
  @Size(max = 500, message = "Başlık en fazla 500 karakter olabilir")
  private String title;

  @NotBlank(message = "İçerik alanı zorunludur")
  @Size(max = 100000, message = "İçerik en fazla 100.000 karakter olabilir")
  private String content;
}
