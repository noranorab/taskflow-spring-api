package com.taskflow.api.common;

import java.util.List;

public record ApiErrorResponse(ApiError error) {

  public record ApiError(String code, String message, List<FieldErrorDetail> details) {
    public ApiError(String code, String message) {
      this(code, message, null);
    }
  }

  public record FieldErrorDetail(String path, String message) {}

  public static ApiErrorResponse of(String code, String message) {
    return new ApiErrorResponse(new ApiError(code, message));
  }

  public static ApiErrorResponse of(String code, String message, List<FieldErrorDetail> details) {
    return new ApiErrorResponse(new ApiError(code, message, details));
  }
}
