package com.taskflow.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.api.common.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public void commence(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
      throws IOException {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    ApiErrorResponse body =
        ApiErrorResponse.of("UNAUTHORIZED", "A valid Bearer token is required for this request");
    response.getWriter().write(objectMapper.writeValueAsString(body));
  }
}
