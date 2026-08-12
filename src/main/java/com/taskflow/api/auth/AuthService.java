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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  private static final Logger log = LoggerFactory.getLogger(AuthService.class);

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuthenticationManager authenticationManager;
  private final JwtService jwtService;
  private final RefreshTokenRepository refreshTokenRepository;
  private final PasswordResetTokenRepository passwordResetTokenRepository;
  private final long refreshTokenTtlDays;
  private final long passwordResetTtlMinutes;
  private final Set<String> adminBootstrapEmails;

  public AuthService(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      AuthenticationManager authenticationManager,
      JwtService jwtService,
      RefreshTokenRepository refreshTokenRepository,
      PasswordResetTokenRepository passwordResetTokenRepository,
      @Value("${app.jwt.refresh-token-ttl-days}") long refreshTokenTtlDays,
      @Value("${app.password-reset.ttl-minutes}") long passwordResetTtlMinutes,
      @Value("${app.admin.bootstrap-emails:}") String adminBootstrapEmailsRaw) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.authenticationManager = authenticationManager;
    this.jwtService = jwtService;
    this.refreshTokenRepository = refreshTokenRepository;
    this.passwordResetTokenRepository = passwordResetTokenRepository;
    this.refreshTokenTtlDays = refreshTokenTtlDays;
    this.passwordResetTtlMinutes = passwordResetTtlMinutes;
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

  @Transactional
  public void forgotPassword(ForgotPasswordRequest request) {
    userRepository
        .findByEmail(request.email())
        .ifPresent(
            user -> {
              String rawToken = OpaqueTokenGenerator.generate();
              PasswordResetToken resetToken =
                  PasswordResetToken.builder()
                      .tokenHash(TokenHasher.sha256Hex(rawToken))
                      .user(user)
                      .expiresAt(Instant.now().plus(Duration.ofMinutes(passwordResetTtlMinutes)))
                      .build();
              passwordResetTokenRepository.save(resetToken);
              log.info(
                  "Password reset requested for {}. Reset token (valid {} min): {}",
                  user.getEmail(),
                  passwordResetTtlMinutes,
                  rawToken);
            });
  }

  @Transactional
  public void resetPassword(ResetPasswordRequest request) {
    String hash = TokenHasher.sha256Hex(request.token());
    PasswordResetToken resetToken =
        passwordResetTokenRepository
            .findByTokenHash(hash)
            .filter(PasswordResetToken::isUsable)
            .orElseThrow(() -> new UnauthorizedException("Invalid or expired reset token"));

    User user = resetToken.getUser();
    user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    userRepository.save(user);

    resetToken.setUsedAt(Instant.now());
    passwordResetTokenRepository.save(resetToken);

    refreshTokenRepository.revokeAllActiveForUser(user, Instant.now());
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
