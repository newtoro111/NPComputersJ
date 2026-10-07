package com.npcomputers.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "cart_line")
public class CartLine {
  @Id public UUID id = UUID.randomUUID();
  public UUID userId;
  public UUID productId;
  public int quantity;
}
