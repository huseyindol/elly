package com.cms.entity.form;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Form schema definition.
 * Contains configuration settings and the list of field definitions.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FormSchema implements Serializable {
  private Map<String, Object> config; // Form configuration (layout, styling, etc.)
  private List<FieldDefinition> fields; // List of field definitions
  private List<StepDefinition> steps; // Step definitions for wizard/multi-step forms
}
