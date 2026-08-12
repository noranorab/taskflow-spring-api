package com.taskflow.api.common;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.taskflow.api.common.ApiErrorResponse.FieldErrorDetail;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
    List<FieldErrorDetail> details =
        ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> new FieldErrorDetail(fe.getField(), fe.getDefaultMessage()))
            .toList();

    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiErrorResponse.of("VALIDATION_ERROR", "Request body failed validation", details));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
    if (ex.getCause() instanceof InvalidFormatException ife && !ife.getPath().isEmpty()) {
      String field = ife.getPath().get(0).getFieldName();
      List<FieldErrorDetail> details =
          List.of(new FieldErrorDetail(field, "must be one of the accepted values"));
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(ApiErrorResponse.of("VALIDATION_ERROR", "Request body failed validation", details));
    }

    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiErrorResponse.of("VALIDATION_ERROR", "Request body is malformed or unreadable"));
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
    String expectedType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "value";
    List<FieldErrorDetail> details =
        List.of(new FieldErrorDetail(ex.getName(), "must be a valid " + expectedType));
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiErrorResponse.of("VALIDATION_ERROR", "Request parameters failed validation", details));
  }

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<ApiErrorResponse> handleNotFound(NotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ApiErrorResponse.of("NOT_FOUND", ex.getMessage()));
  }

  @ExceptionHandler(ConflictException.class)
  public ResponseEntity<ApiErrorResponse> handleConflict(ConflictException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(ApiErrorResponse.of("CONFLICT", ex.getMessage()));
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ApiErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(ApiErrorResponse.of("CONFLICT", "The request conflicts with existing data"));
  }

  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<ApiErrorResponse> handleBadCredentials(BadCredentialsException ex) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(ApiErrorResponse.of("UNAUTHORIZED", "Invalid email or password"));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex) {
    log.error("Unhandled exception", ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiErrorResponse.of("INTERNAL_SERVER_ERROR", "Internal server error"));
  }
}
