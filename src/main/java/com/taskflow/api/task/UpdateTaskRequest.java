package com.taskflow.api.task;

import jakarta.validation.constraints.Size;

public record UpdateTaskRequest(
    @Size(min = 1, max = 200, message = "must be between 1 and 200 characters") String title,
    @Size(max = 10_000, message = "must be at most 10000 characters") String description,
    TaskStatus status,
    TaskPriority priority) {}
