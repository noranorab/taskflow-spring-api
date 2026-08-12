package com.taskflow.api.auth;

import com.taskflow.api.common.ConflictException;
import com.taskflow.api.security.JwtService;
import com.taskflow.api.user.User;
import com.taskflow.api.user.UserRepository;
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

  public AuthService(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      AuthenticationManager authenticationManager,
      JwtService jwtService) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.authenticationManager = authenticationManager;
    this.jwtService = jwtService;
  }

  public AuthResponse register(RegisterRequest request) {
    if (userRepository.existsByEmail(request.email())) {
      throw new ConflictException("An account with this email already exists");
    }

    User user =
        User.builder()
            .email(request.email())
            .passwordHash(passwordEncoder.encode(request.password()))
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
