package com.cms.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
  private boolean result;
  private LocalDateTime timestamp;
  private int status;
  private String error;
  private String errorCode;
  private String message;
  private String path;
  private Map<String, String> validationErrors;
  private List<String> details;

  public static ErrorResponse of(boolean result, int status, String error, String errorCode,
      String message, String path) {
    return ErrorResponse.builder()
        .result(result)
        .timestamp(LocalDateTime.now())
        .status(status)
        .error(error)
        .errorCode(errorCode)
        .message(message)
        .path(path)
        .build();
  }

  public static ErrorResponse withValidationErrors(int status, String message, String path,
      Map<String, String> validationErrors) {
    return ErrorResponse.builder()
        .result(false)
        .timestamp(LocalDateTime.now())
        .status(status)
        .error("Bad Request")
        .errorCode("VALIDATION_ERROR")
        .message(message)
        .path(path)
        .validationErrors(validationErrors)
        .build();
  }

  public static ErrorResponse withDetails(int status, String error, String errorCode,
      String message, String path, List<String> details) {
    return ErrorResponse.builder()
        .result(false)
        .timestamp(LocalDateTime.now())
        .status(status)
        .error(error)
        .errorCode(errorCode)
        .message(message)
        .path(path)
        .details(details)
        .build();
  }
}
