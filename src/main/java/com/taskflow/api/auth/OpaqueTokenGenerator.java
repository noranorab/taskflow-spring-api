package com.taskflow.api.auth;

import java.security.SecureRandom;
import java.util.Base64;

public final class OpaqueTokenGenerator {

  private static final SecureRandom RANDOM = new SecureRandom();
  private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

  private OpaqueTokenGenerator() {}

  public static String generate() {
    byte[] bytes = new byte[32];
    RANDOM.nextBytes(bytes);
    return ENCODER.encodeToString(bytes);
  }
}
