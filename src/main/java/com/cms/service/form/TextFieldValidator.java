package com.cms.service.form;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.springframework.stereotype.Component;

import com.cms.entity.form.FieldDefinition;
import com.cms.entity.form.ValidationRule;
import com.cms.exception.FormValidationException;

/**
 * Validator for text fields.
 * Validates min/max length and regex pattern.
 */
@Component
public class TextFieldValidator implements FieldValidator {

  @Override
  public boolean supports(String type) {
    return "text".equalsIgnoreCase(type) || "textarea".equalsIgnoreCase(type);
  }

  @Override
  public void validate(Object value, FieldDefinition fieldDef) {
    String fieldId = fieldDef.getId();

    // Check required
    if (Boolean.TRUE.equals(fieldDef.getRequired())) {
      if (value == null || value.toString().trim().isEmpty()) {
        throw new FormValidationException(fieldId,
            String.format("'%s' alanı zorunludur", fieldDef.getLabel()));
      }
    }

    // If value is null or empty and not required, skip further validation
    if (value == null || value.toString().trim().isEmpty()) {
      return;
    }

    String strValue = value.toString();
    ValidationRule validation = fieldDef.getValidation();

    if (validation != null) {
      // Check min length
      if (validation.getMin() != null && strValue.length() < validation.getMin()) {
        throw new FormValidationException(fieldId,
            String.format("'%s' alanı en az %d karakter olmalıdır",
                fieldDef.getLabel(), validation.getMin()));
      }

      // Check max length
      if (validation.getMax() != null && strValue.length() > validation.getMax()) {
        throw new FormValidationException(fieldId,
            String.format("'%s' alanı en fazla %d karakter olabilir",
                fieldDef.getLabel(), validation.getMax()));
      }

      // Check regex pattern
      if (validation.getPattern() != null && !validation.getPattern().isEmpty()) {
        try {
          Pattern pattern = Pattern.compile(validation.getPattern());
          if (!pattern.matcher(strValue).matches()) {
            throw new FormValidationException(fieldId,
                String.format("'%s' alanı geçerli bir format değil", fieldDef.getLabel()));
          }
        } catch (PatternSyntaxException e) {
          // Invalid regex pattern in schema - log but don't fail user input
          throw new FormValidationException(fieldId,
              String.format("'%s' alanı için geçersiz validation pattern", fieldDef.getLabel()));
        }
      }
    }
  }
}
