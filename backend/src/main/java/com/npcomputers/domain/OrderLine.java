package com.npcomputers.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_line")
public class OrderLine {
  @Id public UUID id = UUID.randomUUID();
  public UUID orderId;
  public UUID productId;
  public String sku;

  @Column(length = 2000)
  public String description;

  public String condition;
  public int quantity;

  @Column(precision = 12, scale = 2)
  public BigDecimal unitPrice;
}
