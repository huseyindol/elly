package com.cms.entity.form;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Validation rules for form fields.
 * Used for min/max constraints and regex pattern matching.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ValidationRule implements Serializable {
  private Integer min;
  private Integer max;
  private String pattern; // regex pattern
}
