package com.taskflow.api.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTaskRequest(
    @NotBlank(message = "must not be blank")
        @Size(max = 200, message = "must be at most 200 characters")
        String title,
    @Size(max = 10_000, message = "must be at most 10000 characters") String description,
    TaskStatus status,
    TaskPriority priority) {}
