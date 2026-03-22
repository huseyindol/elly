package com.cms.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends BaseException {
  public UnauthorizedException(String message) {
    super(message, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
  }
}
