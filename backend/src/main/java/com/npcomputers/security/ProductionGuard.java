package com.npcomputers.security;

import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class ProductionGuard implements ApplicationRunner {
  private final Environment env;
  private final boolean secure, seed, mail;

  public ProductionGuard(
      Environment env,
      @Value("${app.secure-cookie}") boolean secure,
      @Value("${app.seed}") boolean seed,
      @Value("${app.mail-sink}") boolean mail) {
    this.env = env;
    this.secure = secure;
    this.seed = seed;
    this.mail = mail;
  }

  public void run(ApplicationArguments args) {
    boolean development =
        java.util.Arrays.stream(env.getActiveProfiles())
            .anyMatch(Set.of("local", "demo", "test")::contains);
    if (!development && (!secure || seed || mail))
      throw new IllegalStateException(
          "Production requires secure cookies and disabled demo seed/mail sink.");
  }
}
