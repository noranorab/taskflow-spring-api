package com.taskflow.api.auth;

import jakarta.validation.constraints.NotBlank;

public record LogoutRequest(
    @NotBlank(message = "must not be blank") String refreshToken) {}
