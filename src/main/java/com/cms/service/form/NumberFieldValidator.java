package com.cms.service.form;

import org.springframework.stereotype.Component;

import com.cms.entity.form.FieldDefinition;
import com.cms.entity.form.ValidationRule;
import com.cms.exception.FormValidationException;

/**
 * Validator for number fields.
 * Validates min/max value constraints.
 */
@Component
public class NumberFieldValidator implements FieldValidator {

  @Override
  public boolean supports(String type) {
    return "number".equalsIgnoreCase(type) || "integer".equalsIgnoreCase(type);
  }

  @Override
  public void validate(Object value, FieldDefinition fieldDef) {
    String fieldId = fieldDef.getId();

    // Check required
    if (Boolean.TRUE.equals(fieldDef.getRequired())) {
      if (value == null) {
        throw new FormValidationException(fieldId,
            String.format("'%s' alanı zorunludur", fieldDef.getLabel()));
      }
    }

    // If value is null and not required, skip further validation
    if (value == null) {
      return;
    }

    // Convert to number
    Number numValue;
    try {
      if (value instanceof Number) {
        numValue = (Number) value;
      } else {
        numValue = Double.parseDouble(value.toString());
      }
    } catch (NumberFormatException e) {
      throw new FormValidationException(fieldId,
          String.format("'%s' alanı geçerli bir sayı olmalıdır", fieldDef.getLabel()));
    }

    ValidationRule validation = fieldDef.getValidation();

    if (validation != null) {
      // Check min value
      if (validation.getMin() != null && numValue.doubleValue() < validation.getMin()) {
        throw new FormValidationException(fieldId,
            String.format("'%s' alanı en az %d olmalıdır",
                fieldDef.getLabel(), validation.getMin()));
      }

      // Check max value
      if (validation.getMax() != null && numValue.doubleValue() > validation.getMax()) {
        throw new FormValidationException(fieldId,
            String.format("'%s' alanı en fazla %d olabilir",
                fieldDef.getLabel(), validation.getMax()));
      }
    }
  }
}
