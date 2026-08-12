package com.taskflow.api.auth;

import com.taskflow.api.common.ConflictException;
import com.taskflow.api.common.UnauthorizedException;
import com.taskflow.api.security.JwtService;
import com.taskflow.api.user.Role;
import com.taskflow.api.user.User;
import com.taskflow.api.user.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuthenticationManager authenticationManager;
  private final JwtService jwtService;
  private final RefreshTokenRepository refreshTokenRepository;
  private final long refreshTokenTtlDays;
  private final Set<String> adminBootstrapEmails;

  public AuthService(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      AuthenticationManager authenticationManager,
      JwtService jwtService,
      RefreshTokenRepository refreshTokenRepository,
      @Value("${app.jwt.refresh-token-ttl-days}") long refreshTokenTtlDays,
      @Value("${app.admin.bootstrap-emails:}") String adminBootstrapEmailsRaw) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.authenticationManager = authenticationManager;
    this.jwtService = jwtService;
    this.refreshTokenRepository = refreshTokenRepository;
    this.refreshTokenTtlDays = refreshTokenTtlDays;
    this.adminBootstrapEmails =
        Arrays.stream(adminBootstrapEmailsRaw.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(String::toLowerCase)
            .collect(Collectors.toSet());
  }

  @Transactional
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

    return issueTokenPair(user);
  }

  @Transactional
  public AuthResponse login(LoginRequest request) {
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(request.email(), request.password()));

    User user =
        userRepository
            .findByEmail(request.email())
            .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

    return issueTokenPair(user);
  }

  @Transactional
  public AuthResponse refresh(RefreshRequest request) {
    RefreshToken existing = findUsableRefreshTokenOrThrow(request.refreshToken());

    existing.setRevokedAt(Instant.now());
    refreshTokenRepository.save(existing);

    return issueTokenPair(existing.getUser());
  }

  @Transactional
  public void logout(LogoutRequest request) {
    String hash = TokenHasher.sha256Hex(request.refreshToken());
    refreshTokenRepository
        .findByTokenHash(hash)
        .ifPresent(
            token -> {
              token.setRevokedAt(Instant.now());
              refreshTokenRepository.save(token);
            });
  }

  @Transactional
  public void logoutAll(User currentUser) {
    refreshTokenRepository.revokeAllActiveForUser(currentUser, Instant.now());
  }

  private RefreshToken findUsableRefreshTokenOrThrow(String rawToken) {
    String hash = TokenHasher.sha256Hex(rawToken);
    return refreshTokenRepository
        .findByTokenHash(hash)
        .filter(RefreshToken::isUsable)
        .orElseThrow(() -> new UnauthorizedException("Invalid or expired refresh token"));
  }

  private AuthResponse issueTokenPair(User user) {
    String accessToken = jwtService.generateToken(user.getEmail());
    String rawRefreshToken = OpaqueTokenGenerator.generate();

    RefreshToken refreshToken =
        RefreshToken.builder()
            .tokenHash(TokenHasher.sha256Hex(rawRefreshToken))
            .user(user)
            .expiresAt(Instant.now().plus(Duration.ofDays(refreshTokenTtlDays)))
            .build();
    refreshTokenRepository.save(refreshToken);

    return AuthResponse.of(accessToken, rawRefreshToken, user.getEmail());
  }
}
