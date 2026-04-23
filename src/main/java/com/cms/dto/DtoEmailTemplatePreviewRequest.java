package com.cms.dto;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DtoEmailTemplatePreviewRequest {

  /** Thymeleaf context'ine aktarılacak dummy değişkenler */
  private Map<String, Object> data;
}
