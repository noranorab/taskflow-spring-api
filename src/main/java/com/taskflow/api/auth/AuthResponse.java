package com.taskflow.api.auth;

public record AuthResponse(String accessToken, String refreshToken, String tokenType, String email) {

  public static AuthResponse of(String accessToken, String refreshToken, String email) {
    return new AuthResponse(accessToken, refreshToken, "Bearer", email);
  }
}
