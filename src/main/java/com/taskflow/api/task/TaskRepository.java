package com.taskflow.api.task;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, UUID> {

  List<Task> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

  Optional<Task> findByIdAndUserId(UUID id, UUID userId);
}
