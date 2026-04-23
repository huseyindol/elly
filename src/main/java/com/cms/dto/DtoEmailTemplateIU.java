package com.cms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DtoEmailTemplateIU {

  @NotBlank
  @Size(max = 100)
  @Pattern(regexp = "^[a-z0-9-]+$", message = "templateKey sadece küçük harf, rakam ve tire içermeli")
  private String templateKey;

  @NotBlank
  @Size(max = 255)
  private String subject;

  @NotBlank
  private String htmlBody;

  @Size(max = 500)
  private String description;

  private Boolean active = true;

  private Integer version;

  /** PUT isteğinde optimistic lock kontrolü için gönderilmeli */
  private Long optimisticLockVersion;
}
