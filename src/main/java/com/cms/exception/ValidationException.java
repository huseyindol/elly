package com.cms.exception;

import org.springframework.http.HttpStatus;

public class ValidationException extends BaseException {

  public ValidationException(String message) {
    super(message, HttpStatus.BAD_REQUEST, "VALIDATION_ERROR");
  }

  public ValidationException(String message, Throwable cause) {
    super(message, cause, HttpStatus.BAD_REQUEST, "VALIDATION_ERROR");
  }
}
