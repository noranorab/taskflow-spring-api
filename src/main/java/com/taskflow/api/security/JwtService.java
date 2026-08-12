package com.taskflow.api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  private final SecretKey signingKey;
  private final long accessTokenTtlMinutes;

  public JwtService(
      @Value("${app.jwt.secret}") String secret,
      @Value("${app.jwt.access-token-ttl-minutes}") long accessTokenTtlMinutes) {
    this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.accessTokenTtlMinutes = accessTokenTtlMinutes;
  }

  public String generateToken(String subjectEmail) {
    Instant now = Instant.now();
    return Jwts.builder()
        .subject(subjectEmail)
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plusSeconds(accessTokenTtlMinutes * 60)))
        .signWith(signingKey)
        .compact();
  }

  public Optional<String> extractSubject(String token) {
    try {
      Claims claims = Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
      return Optional.of(claims.getSubject());
    } catch (JwtException | IllegalArgumentException e) {
      return Optional.empty();
    }
  }
}
