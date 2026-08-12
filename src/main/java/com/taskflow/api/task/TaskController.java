package com.taskflow.api.task;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.taskflow.api.user.User;

@RestController
@RequestMapping("/tasks")
@Tag(name = "tasks")
public class TaskController {

  private final TaskService taskService;

  public TaskController(TaskService taskService) {
    this.taskService = taskService;
  }

  @PostMapping
  public ResponseEntity<TaskResponse> createTask(
      @AuthenticationPrincipal User owner, @Valid @RequestBody CreateTaskRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(taskService.createTask(owner, request));
  }

  @GetMapping
  public ResponseEntity<List<TaskResponse>> listTasks(@AuthenticationPrincipal User owner) {
    return ResponseEntity.ok(taskService.listTasks(owner));
  }

  @GetMapping("/{id}")
  public ResponseEntity<TaskResponse> getTask(
      @AuthenticationPrincipal User owner, @PathVariable UUID id) {
    return ResponseEntity.ok(taskService.getTask(owner, id));
  }

  @PatchMapping("/{id}")
  public ResponseEntity<TaskResponse> updateTask(
      @AuthenticationPrincipal User owner,
      @PathVariable UUID id,
      @Valid @RequestBody UpdateTaskRequest request) {
    return ResponseEntity.ok(taskService.updateTask(owner, id, request));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteTask(@AuthenticationPrincipal User owner, @PathVariable UUID id) {
    taskService.deleteTask(owner, id);
    return ResponseEntity.noContent().build();
  }
}
