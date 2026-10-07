package com.npcomputers.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_token")
public class RefreshToken {
  @Id public UUID id = UUID.randomUUID();
  public UUID sessionId;
  public String digest;
  public Instant expiresAt;
  public boolean consumed;
}
