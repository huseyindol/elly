package com.cms.dto;

import com.cms.entity.form.FormSchema;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for creating/updating FormDefinition.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DtoFormDefinitionIU {
  private String title;
  private Integer version;
  private FormSchema schema;
  private Boolean active;
}
