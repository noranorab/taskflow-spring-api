package com.taskflow.api.admin;

import com.taskflow.api.common.ConflictException;
import com.taskflow.api.common.NotFoundException;
import com.taskflow.api.user.User;
import com.taskflow.api.user.UserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserService {

  private final UserRepository userRepository;

  public AdminUserService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Transactional(readOnly = true)
  public List<AdminUserResponse> listUsers() {
    return userRepository.findAll().stream().map(AdminUserResponse::from).toList();
  }

  @Transactional
  public AdminUserResponse updateRole(User currentAdmin, UUID targetUserId, UpdateUserRoleRequest request) {
    if (targetUserId.equals(currentAdmin.getId()) && request.role() != currentAdmin.getRole()) {
      throw new ConflictException("You cannot change your own role");
    }

    User target =
        userRepository
            .findById(targetUserId)
            .orElseThrow(() -> new NotFoundException("User " + targetUserId + " not found"));

    target.setRole(request.role());
    return AdminUserResponse.from(userRepository.save(target));
  }
}
