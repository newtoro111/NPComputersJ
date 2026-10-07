package com.npcomputers.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "customer_order")
public class CustomerOrder {
  @Id public UUID id = UUID.randomUUID();
  public UUID userId;
  public String orderNumber;
  public String status;

  @Column(precision = 12, scale = 2)
  public BigDecimal subtotal;

  @Column(precision = 12, scale = 2)
  public BigDecimal shipping;

  @Column(precision = 12, scale = 2)
  public BigDecimal tax;

  @Column(precision = 12, scale = 2)
  public BigDecimal total;

  @Column(columnDefinition = "text")
  public String shippingSnapshot;

  @Column(columnDefinition = "text")
  public String contactSnapshot;

  public Instant createdAt = Instant.now();
  @Version public long version;
  public String tracking;
}
