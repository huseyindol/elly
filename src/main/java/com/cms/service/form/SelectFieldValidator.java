package com.cms.service.form;

import org.springframework.stereotype.Component;

import com.cms.entity.form.FieldDefinition;
import com.cms.exception.FormValidationException;

/**
 * Validator for select/dropdown fields.
 * Validates required constraint.
 */
@Component
public class SelectFieldValidator implements FieldValidator {

  @Override
  public boolean supports(String type) {
    return "select".equalsIgnoreCase(type) ||
        "dropdown".equalsIgnoreCase(type) ||
        "radio".equalsIgnoreCase(type) ||
        "checkbox".equalsIgnoreCase(type);
  }

  @Override
  public void validate(Object value, FieldDefinition fieldDef) {
    String fieldId = fieldDef.getId();

    // Check required
    if (Boolean.TRUE.equals(fieldDef.getRequired())) {
      if (value == null || value.toString().trim().isEmpty()) {
        throw new FormValidationException(fieldId,
            String.format("'%s' alanı için bir seçim yapmalısınız", fieldDef.getLabel()));
      }
    }

    // Select fields typically don't have additional validation
    // Options validation could be added here if needed (e.g., check if value is in
    // allowed options)
  }
}
