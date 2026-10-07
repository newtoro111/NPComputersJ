package com.npcomputers.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "app_user")
public class User {
  @Id public UUID id = UUID.randomUUID();
  public String email;
  public String passwordHash;
  public long accountNumber;
  public String role;
  public boolean enabled = true;
  public long securityVersion;
  public Instant createdAt = Instant.now();
  public Instant lastAccess;
}
