package com.cms.dto;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for form submission request.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DtoFormSubmit {
  private Map<String, Object> payload;
}
