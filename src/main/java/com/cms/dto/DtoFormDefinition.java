package com.cms.dto;

import java.util.Date;

import com.cms.entity.form.FormSchema;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for FormDefinition responses.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DtoFormDefinition {
  private Long id;
  private String title;
  private Integer version;
  private FormSchema schema;
  private Boolean active;
  private Date createdAt;
  private Date updatedAt;
}
