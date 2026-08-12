package com.taskflow.api.admin;

import com.taskflow.api.user.Role;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRoleRequest(@NotNull(message = "must not be null") Role role) {}
