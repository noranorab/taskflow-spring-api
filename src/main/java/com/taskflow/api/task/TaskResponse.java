package com.taskflow.api.task;

import java.time.Instant;
import java.util.UUID;

public record TaskResponse(
    UUID id,
    String title,
    String description,
    TaskStatus status,
    TaskPriority priority,
    Instant createdAt,
    Instant updatedAt) {

  public static TaskResponse from(Task task) {
    return new TaskResponse(
        task.getId(),
        task.getTitle(),
        task.getDescription(),
        task.getStatus(),
        task.getPriority(),
        task.getCreatedAt(),
        task.getUpdatedAt());
  }
}
