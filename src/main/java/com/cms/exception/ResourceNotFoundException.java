package com.cms.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends BaseException {

  public ResourceNotFoundException(String message) {
    super(message, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
  }

  public ResourceNotFoundException(String resourceName, Long id) {
    super(String.format("%s with id %d not found", resourceName, id),
        HttpStatus.NOT_FOUND,
        "RESOURCE_NOT_FOUND");
  }

  public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
    super(String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue),
        HttpStatus.NOT_FOUND,
        "RESOURCE_NOT_FOUND");
  }
}
