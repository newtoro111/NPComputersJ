package com.npcomputers.service;

import com.npcomputers.api.ApiException;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class SimulatedPaymentService implements PaymentService {
  public Result pay(UUID order, BigDecimal amount, String method, String scenario) {
    if (!Set.of("APPROVE", "DECLINE").contains(scenario)
        || !Set.of("SIMULATED_CARD", "SIMULATED_WALLET").contains(method)
        || amount.signum() <= 0)
      throw new ApiException(400, "PAYMENT_INPUT", "Invalid simulated payment request.");
    return new Result(scenario.equals("APPROVE"), "SIM-" + order);
  }
}
