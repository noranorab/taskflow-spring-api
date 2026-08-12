package com.taskflow.api.config;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * PaaS platforms (Railway, Render, Heroku) inject a single DATABASE_URL in
 * "postgres://user:pass@host:port/db" form, but Spring's JDBC driver expects
 * a jdbc:postgresql:// URL plus separate username/password. This translates
 * one into the other when DATABASE_URL is present, so no per-platform
 * datasource variable wiring is needed at deploy time.
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

  @Override
  public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
    String databaseUrl = environment.getProperty("DATABASE_URL");
    if (databaseUrl == null || !databaseUrl.startsWith("postgres")) {
      return;
    }

    URI uri = URI.create(databaseUrl);
    int port = uri.getPort() == -1 ? 5432 : uri.getPort();
    String jdbcUrl = "jdbc:postgresql://" + uri.getHost() + ":" + port + uri.getPath();

    Map<String, Object> overrides = new HashMap<>();
    overrides.put("spring.datasource.url", jdbcUrl);

    String userInfo = uri.getUserInfo();
    if (userInfo != null && userInfo.contains(":")) {
      String[] parts = userInfo.split(":", 2);
      overrides.put("spring.datasource.username", parts[0]);
      overrides.put("spring.datasource.password", parts[1]);
    }

    environment.getPropertySources().addFirst(new MapPropertySource("databaseUrlOverride", overrides));
  }
}
