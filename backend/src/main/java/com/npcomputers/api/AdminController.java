package com.npcomputers.api;

import static com.npcomputers.security.PrincipalSupport.user;

import com.npcomputers.api.Contracts.*;
import com.npcomputers.service.*;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
  private final AdminService s;
  private final CatalogService c;
  private final Support support;
  private final boolean mail;

  public AdminController(
      AdminService s,
      CatalogService c,
      Support support,
      @org.springframework.beans.factory.annotation.Value("${app.mail-sink}") boolean mail) {
    this.s = s;
    this.c = c;
    this.support = support;
    this.mail = mail;
  }

  @GetMapping("/users")
  Object users(@RequestParam(defaultValue = "0") int page) {
    return s.users(page);
  }

  @PostMapping("/users/{id}/roles")
  void role(@PathVariable UUID id, @Valid @RequestBody Role r) {
    s.role(user(), id, r.role());
  }

  @PostMapping("/users/{id}/enabled")
  void enable(@PathVariable UUID id, @RequestBody Enabled e) {
    s.enabled(user(), id, e.enabled());
  }

  @GetMapping("/products")
  Object products(@RequestParam(defaultValue = "0") int page) {
    return c.list("", "", "", "", BigDecimal.ZERO, new BigDecimal("9999999999"), page, 100, true);
  }

  @PostMapping("/products")
  Object create(@Valid @RequestBody ProductInput r) {
    return c.save(user(), null, r);
  }

  @PutMapping("/products/{id}")
  Object update(@PathVariable UUID id, @Valid @RequestBody ProductInput r) {
    return c.save(user(), id, r);
  }

  @PostMapping("/inventory/adjustments")
  Object adjust(@Valid @RequestBody Adjustment r) {
    return s.adjust(user(), r);
  }

  @GetMapping("/inventory/replenishments")
  Object replenish() {
    return s.replenishment();
  }

  @PostMapping("/inventory/receipts")
  Object receipt(@Valid @RequestBody Receipt r) {
    return s.receive(user(), r);
  }

  @GetMapping("/audit")
  Object audit(@RequestParam(defaultValue = "0") int page) {
    return s.audit(page);
  }

  @GetMapping("/mail-sink")
  Object mail() {
    if (!mail) throw new ApiException(404, "DISABLED", "Mail sink is disabled.");
    return support.db.queryForList("select * from mail_sink order by created_at desc limit 100");
  }
}
