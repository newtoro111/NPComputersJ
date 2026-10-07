package com.npcomputers.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auth_session")
public class AuthSession {
  @Id public UUID id = UUID.randomUUID();
  public UUID userId;
  public long securityVersion;
  public Instant createdAt = Instant.now();
  public Instant expiresAt;
  public Instant lastUsed;
  public boolean revoked;
}
