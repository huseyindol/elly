package com.cms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DtoMailTestRequest {

  @NotBlank(message = "Test alıcısı zorunludur")
  @Email(message = "Geçersiz e-posta formatı")
  private String testTo;
}
