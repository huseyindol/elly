package com.cms.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends BaseException {

  public ConflictException(String message) {
    super(message, HttpStatus.CONFLICT, "CONFLICT");
  }

  public ConflictException(String message, Throwable cause) {
    super(message, cause, HttpStatus.CONFLICT, "CONFLICT");
  }
}
