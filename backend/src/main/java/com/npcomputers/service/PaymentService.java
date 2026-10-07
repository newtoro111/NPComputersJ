package com.npcomputers.service;

import java.math.BigDecimal;
import java.util.UUID;

public interface PaymentService {
  record Result(boolean approved, String reference) {}

  Result pay(UUID order, BigDecimal amount, String method, String scenario);
}
