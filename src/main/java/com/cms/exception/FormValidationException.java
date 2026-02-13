package com.cms.exception;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;

import lombok.Getter;

/**
 * Exception thrown when form validation fails.
 * Contains a list of field-specific validation errors.
 */
@Getter
public class FormValidationException extends BaseException {

  private final List<FieldError> fieldErrors;

  public FormValidationException(String message) {
    super(message, HttpStatus.BAD_REQUEST, "FORM_VALIDATION_ERROR");
    this.fieldErrors = new ArrayList<>();
  }

  public FormValidationException(String message, List<FieldError> fieldErrors) {
    super(message, HttpStatus.BAD_REQUEST, "FORM_VALIDATION_ERROR");
    this.fieldErrors = fieldErrors != null ? fieldErrors : new ArrayList<>();
  }

  public FormValidationException(String fieldId, String fieldMessage) {
    super(fieldMessage, HttpStatus.BAD_REQUEST, "FORM_VALIDATION_ERROR");
    this.fieldErrors = new ArrayList<>();
    this.fieldErrors.add(new FieldError(fieldId, fieldMessage));
  }

  /**
   * Represents a validation error for a specific field.
   */
  @Getter
  public static class FieldError {
    private final String fieldId;
    private final String message;

    public FieldError(String fieldId, String message) {
      this.fieldId = fieldId;
      this.message = message;
    }
  }
}
