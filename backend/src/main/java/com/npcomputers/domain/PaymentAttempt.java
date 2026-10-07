package com.npcomputers.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_attempt")
public class PaymentAttempt {
  @Id public UUID id = UUID.randomUUID();
  public UUID orderId;
  public String method;
  public String status;
  public String transactionRef;

  @Column(precision = 12, scale = 2)
  public BigDecimal amount;

  public Instant createdAt = Instant.now();
}
