package com.cms.exception;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  /**
   * Handle FormValidationException with field-level errors
   */
  @ExceptionHandler(FormValidationException.class)
  public ResponseEntity<ErrorResponse> handleFormValidationException(FormValidationException ex,
      HttpServletRequest request) {
    log.error("Form validation error: {}", ex.getMessage());

    Map<String, String> validationErrors = new HashMap<>();
    if (ex.getFieldErrors() != null) {
      for (FormValidationException.FieldError fieldError : ex.getFieldErrors()) {
        validationErrors.put(fieldError.getFieldId(), fieldError.getMessage());
      }
    }

    ErrorResponse errorResponse = ErrorResponse.withValidationErrors(
        HttpStatus.BAD_REQUEST.value(),
        ex.getMessage(),
        request.getRequestURI(),
        validationErrors);

    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
  }

  /**
   * Handle custom BaseException and its subclasses
   */
  @ExceptionHandler(BaseException.class)
  public ResponseEntity<ErrorResponse> handleBaseException(BaseException ex, HttpServletRequest request) {
    log.error("BaseException occurred: {}", ex.getMessage(), ex);

    ErrorResponse errorResponse = ErrorResponse.of(
        false,
        ex.getStatus().value(),
        ex.getStatus().getReasonPhrase(),
        ex.getErrorCode(),
        ex.getMessage(),
        request.getRequestURI());

    return new ResponseEntity<>(errorResponse, ex.getStatus());
  }

  /**
   * Handle ResourceNotFoundException
   */
  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex,
      HttpServletRequest request) {
    log.error("Resource not found: {}", ex.getMessage());

    ErrorResponse errorResponse = ErrorResponse.of(
        false,
        HttpStatus.NOT_FOUND.value(),
        HttpStatus.NOT_FOUND.getReasonPhrase(),
        ex.getErrorCode(),
        ex.getMessage(),
        request.getRequestURI());

    return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
  }

  /**
   * Handle validation errors from @Valid annotation
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
      HttpServletRequest request) {
    log.error("Validation error: {}", ex.getMessage());

    Map<String, String> validationErrors = new HashMap<>();
    ex.getBindingResult().getAllErrors().forEach(error -> {
      String fieldName = ((FieldError) error).getField();
      String errorMessage = error.getDefaultMessage();
      validationErrors.put(fieldName, errorMessage);
    });

    ErrorResponse errorResponse = ErrorResponse.withValidationErrors(
        HttpStatus.BAD_REQUEST.value(),
        "Validation failed",
        request.getRequestURI(),
        validationErrors);

    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
  }

  /**
   * Handle constraint violation exceptions
   */
  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex,
      HttpServletRequest request) {
    log.error("Constraint violation: {}", ex.getMessage());

    Map<String, String> validationErrors = new HashMap<>();
    for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
      String propertyPath = violation.getPropertyPath().toString();
      String message = violation.getMessage();
      validationErrors.put(propertyPath, message);
    }

    ErrorResponse errorResponse = ErrorResponse.withValidationErrors(
        HttpStatus.BAD_REQUEST.value(),
        "Constraint violation",
        request.getRequestURI(),
        validationErrors);

    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
  }

  /**
   * Handle DataIntegrityViolationException (database constraint violations)
   */
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex,
      HttpServletRequest request) {
    log.error("Data integrity violation: {}", ex.getMessage(), ex);

    String message = "Database constraint violation";
    if (ex.getMessage().contains("unique constraint") || ex.getMessage().contains("duplicate key")) {
      message = "A record with this value already exists";
    } else if (ex.getMessage().contains("foreign key constraint")) {
      message = "Cannot perform operation due to related records";
    }

    ErrorResponse errorResponse = ErrorResponse.of(
        false,
        HttpStatus.CONFLICT.value(),
        HttpStatus.CONFLICT.getReasonPhrase(),
        "DATA_INTEGRITY_VIOLATION",
        message,
        request.getRequestURI());

    return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
  }

  /**
   * Handle HTTP message not readable exception
   */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
      HttpServletRequest request) {
    log.error("HTTP message not readable: {}", ex.getMessage());

    ErrorResponse errorResponse = ErrorResponse.of(
        false,
        HttpStatus.BAD_REQUEST.value(),
        HttpStatus.BAD_REQUEST.getReasonPhrase(),
        "INVALID_REQUEST_BODY",
        "Invalid request body format",
        request.getRequestURI());

    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
  }

  /**
   * Handle method argument type mismatch
   */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex,
      HttpServletRequest request) {
    log.error("Method argument type mismatch: {}", ex.getMessage());

    String message = String.format("Parameter '%s' should be of type %s",
        ex.getName(),
        ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");

    ErrorResponse errorResponse = ErrorResponse.of(
        false,
        HttpStatus.BAD_REQUEST.value(),
        HttpStatus.BAD_REQUEST.getReasonPhrase(),
        "INVALID_PARAMETER_TYPE",
        message,
        request.getRequestURI());

    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
  }

  /**
   * Handle missing request parameter
   */
  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ErrorResponse> handleMissingServletRequestParameter(
      MissingServletRequestParameterException ex,
      HttpServletRequest request) {
    log.error("Missing request parameter: {}", ex.getMessage());

    String message = String.format("Required parameter '%s' is missing", ex.getParameterName());

    ErrorResponse errorResponse = ErrorResponse.of(
        false,
        HttpStatus.BAD_REQUEST.value(),
        HttpStatus.BAD_REQUEST.getReasonPhrase(),
        "MISSING_PARAMETER",
        message,
        request.getRequestURI());

    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
  }

  /**
   * Handle HTTP request method not supported
   */
  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupported(
      HttpRequestMethodNotSupportedException ex,
      HttpServletRequest request) {
    log.error("HTTP request method not supported: {}", ex.getMessage());

    String supportedMethods = ex.getSupportedHttpMethods() != null
        ? ex.getSupportedHttpMethods().stream().map(Object::toString).collect(Collectors.joining(", "))
        : "";

    String message = String.format("Request method '%s' not supported. Supported methods: %s",
        ex.getMethod(), supportedMethods);

    ErrorResponse errorResponse = ErrorResponse.of(
        false,
        HttpStatus.METHOD_NOT_ALLOWED.value(),
        HttpStatus.METHOD_NOT_ALLOWED.getReasonPhrase(),
        "METHOD_NOT_ALLOWED",
        message,
        request.getRequestURI());

    return new ResponseEntity<>(errorResponse, HttpStatus.METHOD_NOT_ALLOWED);
  }

  /**
   * Handle NoHandlerFoundException and NoResourceFoundException (404)
   */
  @ExceptionHandler({ NoHandlerFoundException.class, NoResourceFoundException.class })
  public ResponseEntity<ErrorResponse> handleNoHandlerFound(Exception ex, HttpServletRequest request) {
    log.error("No handler found: {}", ex.getMessage());

    ErrorResponse errorResponse = ErrorResponse.of(
        false,
        HttpStatus.NOT_FOUND.value(),
        HttpStatus.NOT_FOUND.getReasonPhrase(),
        "ENDPOINT_NOT_FOUND",
        "The requested endpoint does not exist",
        request.getRequestURI());

    return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
  }

  /**
   * Handle Spring Security Authentication exceptions
   */
  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex,
      HttpServletRequest request) {
    log.error("Authentication failed: {}", ex.getMessage());

    String message = "Authentication failed";
    String errorCode = "AUTHENTICATION_FAILED";
    HttpStatus status = HttpStatus.UNAUTHORIZED;

    if (ex instanceof BadCredentialsException) {
      message = "Invalid username/email or password";
      errorCode = "BAD_CREDENTIALS";
    } else if (ex instanceof DisabledException) {
      message = "User account is disabled";
      errorCode = "ACCOUNT_DISABLED";
    } else if (ex instanceof LockedException) {
      message = "User account is locked";
      errorCode = "ACCOUNT_LOCKED";
    }

    ErrorResponse errorResponse = ErrorResponse.of(
        false,
        status.value(),
        status.getReasonPhrase(),
        errorCode,
        message,
        request.getRequestURI());

    return new ResponseEntity<>(errorResponse, status);
  }

  /**
   * Handle generic RuntimeException
   */
  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex, HttpServletRequest request) {
    log.error("Runtime exception occurred: {}", ex.getMessage(), ex);

    ErrorResponse errorResponse = ErrorResponse.of(
        false,
        HttpStatus.INTERNAL_SERVER_ERROR.value(),
        HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
        "RUNTIME_ERROR",
        ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred",
        request.getRequestURI());

    return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  /**
   * Handle all other exceptions
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleAllUncaughtException(Exception ex, HttpServletRequest request) {
    log.error("Uncaught exception occurred: {}", ex.getMessage(), ex);

    ErrorResponse errorResponse = ErrorResponse.of(
        false,
        HttpStatus.INTERNAL_SERVER_ERROR.value(),
        HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
        "INTERNAL_SERVER_ERROR",
        "An unexpected error occurred. Please contact support.",
        request.getRequestURI());

    return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
