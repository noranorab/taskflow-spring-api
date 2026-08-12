package com.taskflow.api.task;

import com.taskflow.api.common.NotFoundException;
import com.taskflow.api.user.User;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {

  private final TaskRepository taskRepository;

  public TaskService(TaskRepository taskRepository) {
    this.taskRepository = taskRepository;
  }

  @Transactional
  public TaskResponse createTask(User owner, CreateTaskRequest request) {
    Task task =
        Task.builder()
            .title(request.title())
            .description(request.description())
            .status(request.status() != null ? request.status() : TaskStatus.TODO)
            .priority(request.priority() != null ? request.priority() : TaskPriority.NO_PRIORITY)
            .user(owner)
            .build();

    return TaskResponse.from(taskRepository.save(task));
  }

  @Transactional(readOnly = true)
  public List<TaskResponse> listTasks(User owner) {
    return taskRepository.findAllByUserIdOrderByCreatedAtDesc(owner.getId()).stream()
        .map(TaskResponse::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public TaskResponse getTask(User owner, UUID id) {
    return TaskResponse.from(findOwnedOrThrow(owner, id));
  }

  @Transactional
  public TaskResponse updateTask(User owner, UUID id, UpdateTaskRequest request) {
    Task task = findOwnedOrThrow(owner, id);

    if (request.title() != null) {
      task.setTitle(request.title());
    }
    if (request.description() != null) {
      task.setDescription(request.description());
    }
    if (request.status() != null) {
      task.setStatus(request.status());
    }
    if (request.priority() != null) {
      task.setPriority(request.priority());
    }

    return TaskResponse.from(task);
  }

  @Transactional
  public void deleteTask(User owner, UUID id) {
    Task task = findOwnedOrThrow(owner, id);
    taskRepository.delete(task);
  }

  private Task findOwnedOrThrow(User owner, UUID id) {
    return taskRepository
        .findByIdAndUserId(id, owner.getId())
        .orElseThrow(() -> new NotFoundException("Task " + id + " not found"));
  }
}
