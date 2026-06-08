package com.cms.exception;

import org.springframework.http.HttpStatus;

/** Tenant depolama kotası aşıldığında — HTTP 413 (Payload Too Large). */
public class QuotaExceededException extends BaseException {

  public QuotaExceededException(String message) {
    super(message, HttpStatus.PAYLOAD_TOO_LARGE, "STORAGE_QUOTA_EXCEEDED");
  }
}
