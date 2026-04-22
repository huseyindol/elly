package com.cms.dto;

import java.util.Map;

import jakarta.validation.constraints.Email;
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
public class EmailRequest {

  @NotBlank(message = "Recipient email is required")
  @Email(message = "Invalid email format")
  private String to;

  @NotBlank(message = "Subject is required")
  private String subject;

  @NotBlank(message = "Template name is required")
  private String templateName;

  private Map<String, Object> dynamicData;

  /**
   * Mail+Form v1: Varsayilan hesap kaldirildi; mailAccountId zorunludur.
   * Hesap tenant'a ait olmali ve aktif olmalidir; aksi halde 422 donulur.
   */
  @NotNull(message = "mailAccountId zorunludur (varsayilan hesap kaldirildi)")
  private Long mailAccountId;
}
