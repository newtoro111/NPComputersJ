package com.npcomputers.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "product")
public class Product {
  @Id public UUID id = UUID.randomUUID();
  public String sku;
  public String manufacturer;
  public String model;
  public String category;
  public String condition;
  public String cpu;
  public String ram;
  public String storage;
  public String gpu;
  public String os;

  @Column(precision = 12, scale = 2)
  public BigDecimal price;

  public int stock;
  public boolean active = true;

  @Column(length = 2000)
  public String description;

  public int reorderPoint = 100;
  public int reorderQuantity = 100;
  @Version public long version;
}
