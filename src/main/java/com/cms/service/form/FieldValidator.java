package com.cms.service.form;

import com.cms.entity.form.FieldDefinition;

/**
 * Strategy interface for field validation.
 * Implementations should provide type-specific validation logic.
 */
public interface FieldValidator {

  /**
   * Check if this validator supports the given field type.
   * 
   * @param type the field type (text, number, select, etc.)
   * @return true if this validator can handle the field type
   */
  boolean supports(String type);

  /**
   * Validate the field value against the field definition rules.
   * 
   * @param value    the submitted field value
   * @param fieldDef the field definition containing validation rules
   * @throws com.cms.exception.FormValidationException if validation fails
   */
  void validate(Object value, FieldDefinition fieldDef);
}
