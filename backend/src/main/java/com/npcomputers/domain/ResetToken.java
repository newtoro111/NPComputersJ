package com.npcomputers.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reset_token")
public class ResetToken {
  @Id public UUID id = UUID.randomUUID();
  public UUID userId;
  public String digest;
  public Instant expiresAt;
  public boolean consumed;
}
