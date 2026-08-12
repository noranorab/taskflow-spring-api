package com.taskflow.api.auth;

public record AuthResponse(String accessToken, String tokenType, String email) {

  public static AuthResponse of(String accessToken, String email) {
    return new AuthResponse(accessToken, "Bearer", email);
  }
}
