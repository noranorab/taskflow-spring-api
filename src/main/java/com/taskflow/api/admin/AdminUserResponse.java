package com.taskflow.api.admin;

import com.taskflow.api.user.Role;
import com.taskflow.api.user.User;
import java.time.Instant;
import java.util.UUID;

public record AdminUserResponse(UUID id, String email, Role role, Instant createdAt) {

  public static AdminUserResponse from(User user) {
    return new AdminUserResponse(user.getId(), user.getEmail(), user.getRole(), user.getCreatedAt());
  }
}
