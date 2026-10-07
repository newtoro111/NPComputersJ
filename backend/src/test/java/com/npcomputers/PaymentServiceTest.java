package com.npcomputers;

import static org.junit.jupiter.api.Assertions.*;

import com.npcomputers.api.ApiException;
import com.npcomputers.service.*;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.*;

class PaymentServiceTest {
  private final PaymentService payment = new SimulatedPaymentService();

  @Test
  @DisplayName("PAY01 deterministic approved and declined payments use safe references")
  void outcomes() {
    UUID id = UUID.randomUUID();
    var a = payment.pay(id, new BigDecimal("10.99"), "SIMULATED_CARD", "APPROVE");
    var b = payment.pay(id, new BigDecimal("10.99"), "SIMULATED_WALLET", "DECLINE");
    assertTrue(a.approved());
    assertFalse(b.approved());
    assertEquals("SIM-" + id, a.reference());
  }

  @Test
  @DisplayName("PAY01 rejects real payment methods and invalid amounts")
  void invalid() {
    assertThrows(
        ApiException.class,
        () -> payment.pay(UUID.randomUUID(), BigDecimal.ONE, "CREDIT_CARD", "APPROVE"));
    assertThrows(
        ApiException.class,
        () -> payment.pay(UUID.randomUUID(), BigDecimal.ZERO, "SIMULATED_CARD", "APPROVE"));
  }

  @Test
  @DisplayName("ACC01 long passwords and Unicode are allowed without truncation")
  void password() {
    assertDoesNotThrow(() -> AuthService.validatePassword("long password with symbols é!"));
    assertDoesNotThrow(() -> AuthService.validatePassword("a".repeat(128)));
    assertThrows(ApiException.class, () -> AuthService.validatePassword("short"));
  }
}
