package com.cms.entity.form;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Field definition within a form schema.
 * Defines the structure, type, and validation rules for a single form field.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FieldDefinition implements Serializable {
  private String id; // Unique field identifier
  private String type; // Field type: text, select, number
  private String label; // Display label
  private Boolean required; // Whether field is required
  private ValidationRule validation; // Validation rules
  private ConditionRule condition; // Conditional visibility rule
}
