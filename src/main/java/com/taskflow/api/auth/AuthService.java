package com.taskflow.api.auth;

import com.taskflow.api.common.ConflictException;
import com.taskflow.api.security.JwtService;
import com.taskflow.api.user.Role;
import com.taskflow.api.user.User;
import com.taskflow.api.user.UserRepository;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuthenticationManager authenticationManager;
  private final JwtService jwtService;
  private final Set<String> adminBootstrapEmails;

  public AuthService(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      AuthenticationManager authenticationManager,
      JwtService jwtService,
      @Value("${app.admin.bootstrap-emails:}") String adminBootstrapEmailsRaw) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.authenticationManager = authenticationManager;
    this.jwtService = jwtService;
    this.adminBootstrapEmails =
        Arrays.stream(adminBootstrapEmailsRaw.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(String::toLowerCase)
            .collect(Collectors.toSet());
  }

  public AuthResponse register(RegisterRequest request) {
    if (userRepository.existsByEmail(request.email())) {
      throw new ConflictException("An account with this email already exists");
    }

    Role role =
        adminBootstrapEmails.contains(request.email().toLowerCase()) ? Role.ADMIN : Role.USER;

    User user =
        User.builder()
            .email(request.email())
            .passwordHash(passwordEncoder.encode(request.password()))
            .role(role)
            .build();

    userRepository.save(user);

    return AuthResponse.of(jwtService.generateToken(user.getEmail()), user.getEmail());
  }

  public AuthResponse login(LoginRequest request) {
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(request.email(), request.password()));

    return AuthResponse.of(jwtService.generateToken(request.email()), request.email());
  }
}
