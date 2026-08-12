package com.taskflow.api.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(
    @NotBlank(message = "must not be blank") @Email(message = "must be a valid email") String email) {}
