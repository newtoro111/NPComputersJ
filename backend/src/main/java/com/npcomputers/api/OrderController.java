package com.npcomputers.api;

import static com.npcomputers.security.PrincipalSupport.*;

import com.npcomputers.api.Contracts.*;
import com.npcomputers.service.OrderService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class OrderController {
  private final OrderService s;

  public OrderController(OrderService s) {
    this.s = s;
  }

  @GetMapping("/cart")
  Object cart() {
    return s.cart(user());
  }

  @PutMapping("/cart/items/{id}")
  Object quantity(@PathVariable UUID id, @Valid @RequestBody Quantity q) {
    return s.quantity(user(), id, q.quantity());
  }

  @DeleteMapping("/cart/items/{id}")
  ResponseEntity<?> remove(@PathVariable UUID id) {
    s.remove(user(), id);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/cart")
  ResponseEntity<?> clear() {
    s.clear(user());
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/checkout/quote")
  Object quote(@Valid @RequestBody Quote q) {
    return s.quote(user(), q);
  }

  @PostMapping("/orders")
  @PreAuthorize("hasRole('CUSTOMER')")
  ResponseEntity<?> place(
      @RequestHeader("Idempotency-Key") String key, @Valid @RequestBody PlaceOrder r) {
    return ResponseEntity.status(201).body(s.place(user(), key, r));
  }

  @GetMapping("/orders")
  Object list(@RequestParam(defaultValue = "0") int page) {
    return s.list(user(), staff(), page);
  }

  @GetMapping("/orders/{id}")
  Object detail(@PathVariable UUID id) {
    return s.detail(user(), id, staff());
  }

  @PatchMapping("/staff/orders/{id}/status")
  @PreAuthorize("hasAnyRole('ADMIN','SALES')")
  Object status(@PathVariable UUID id, @Valid @RequestBody Status r) {
    return s.status(user(), id, r);
  }
}
