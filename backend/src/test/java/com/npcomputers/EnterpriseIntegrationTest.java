package com.npcomputers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.npcomputers.api.*;
import com.npcomputers.api.Contracts.*;
import com.npcomputers.domain.*;
import com.npcomputers.persistence.*;
import com.npcomputers.service.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EnabledIfSystemProperty(named = "np.integration", matches = "true")
class EnterpriseIntegrationTest {
  @Autowired AuthService auth;
  @Autowired CustomerService customers;
  @Autowired OrderService orders;
  @Autowired CatalogService catalog;
  @Autowired AdminService admin;
  @Autowired Support s;
  @Autowired ProductRepository products;
  @Autowired UserRepository users;
  @Autowired AuthSessionRepository sessions;
  @Autowired MockMvc mvc;
  @MockitoSpyBean SimulatedPaymentService simulator;
  private final String password = "Test account password 123!";
  private final Address address =
      new Address("123 Main Street", "", "Overland Park", "KS", "66251", "US");

  UUID customer() {
    var u =
        auth.register(
            new Registration("test-" + UUID.randomUUID() + "@example.com", password, password));
    UUID id = (UUID) u.get("id");
    customers.save(
        id,
        new Profile(
            "INDIVIDUAL",
            "",
            new Contact("Demo", "Customer", "demo@example.com", "913-555-0100"),
            null,
            address,
            address,
            0));
    return id;
  }

  AuthService.Result login(UUID user) {
    return auth.login(new Credentials(users.findById(user).orElseThrow().email, password));
  }

  Product product(int stock) {
    var p = new Product();
    p.sku = "TEST-" + UUID.randomUUID();
    p.manufacturer = "Lenovo";
    p.model = "Test computer";
    p.category = "LAPTOP";
    p.condition = "NEW";
    p.cpu = "CPU";
    p.ram = "16 GB";
    p.storage = "512 GB";
    p.os = "OS";
    p.price = new BigDecimal("100.01");
    p.stock = stock;
    p.description = "Test product";
    return products.saveAndFlush(p);
  }

  @SuppressWarnings("unchecked")
  PlaceOrder request(UUID user, String scenario) {
    var q = (Map<String, Object>) orders.quote(user, new Quote(address));
    return new PlaceOrder(
        (UUID) q.get("id"), (String) q.get("digest"), address, "SIMULATED_CARD", scenario);
  }

  @SuppressWarnings("unchecked")
  Map<String, Object> place(UUID user, String key, PlaceOrder r) {
    return (Map<String, Object>) orders.place(user, key, r);
  }

  @Test
  @DisplayName("ORD04 duplicate request returns original order and decrements once")
  void idempotency() {
    UUID u = customer();
    var p = product(5);
    orders.quantity(u, p.id, 2);
    var r = request(u, "APPROVE");
    String key = UUID.randomUUID().toString();
    var a = place(u, key, r);
    var b = place(u, key, r);
    assertEquals(a.get("id"), b.get("id"));
    assertEquals("CONFIRMED", a.get("status"));
    assertEquals(new BigDecimal("210.02"), a.get("total"));
    assertEquals(3, products.findById(p.id).orElseThrow().stock);
    assertEquals(
        1,
        s.db.queryForObject(
            "select count(*) from stock_movement where product_id=?", Integer.class, p.id));
    assertTrue(((List<?>) orders.cart(u)).isEmpty());
  }

  @Test
  @DisplayName("ORD04 changed payload with same key conflicts")
  void keyReuse() {
    UUID u = customer();
    var p = product(5);
    orders.quantity(u, p.id, 1);
    var r = request(u, "APPROVE");
    String key = UUID.randomUUID().toString();
    place(u, key, r);
    var different =
        new PlaceOrder(r.quoteId(), r.quoteDigest(), address, r.paymentMethod(), "DECLINE");
    assertEquals(409, assertThrows(ApiException.class, () -> place(u, key, different)).status);
  }

  @Test
  @DisplayName("ORD05 decline retains order and cart without inventory mutation")
  void decline() {
    UUID u = customer();
    var p = product(5);
    orders.quantity(u, p.id, 2);
    var o = place(u, UUID.randomUUID().toString(), request(u, "DECLINE"));
    assertEquals("PAYMENT_FAILED", o.get("status"));
    assertEquals(5, products.findById(p.id).orElseThrow().stock);
    assertFalse(((List<?>) orders.cart(u)).isEmpty());
    assertEquals(
        0,
        s.db.queryForObject(
            "select count(*) from stock_movement where product_id=?", Integer.class, p.id));
  }

  @Test
  @DisplayName("ORD03 changed price invalidates quote before payment")
  void changedQuote() {
    UUID u = customer();
    var p = product(5);
    orders.quantity(u, p.id, 1);
    var r = request(u, "APPROVE");
    p.price = new BigDecimal("120.00");
    products.saveAndFlush(p);
    var e = assertThrows(ApiException.class, () -> place(u, UUID.randomUUID().toString(), r));
    assertEquals("QUOTE_CHANGED", e.code);
    assertEquals(5, products.findById(p.id).orElseThrow().stock);
  }

  @Test
  @DisplayName("ORD05 unexpected simulator failure rolls back all order state")
  void rollback() {
    UUID u = customer();
    var p = product(5);
    orders.quantity(u, p.id, 1);
    var r = request(u, "APPROVE");
    doThrow(new IllegalStateException("Injected failure"))
        .when(simulator)
        .pay(any(), any(), any(), any());
    assertThrows(IllegalStateException.class, () -> place(u, UUID.randomUUID().toString(), r));
    assertEquals(
        0,
        s.db.queryForObject(
            "select count(*) from customer_order where user_id=?", Integer.class, u));
    assertEquals(5, products.findById(p.id).orElseThrow().stock);
    assertFalse(((List<?>) orders.cart(u)).isEmpty());
  }

  @Test
  @DisplayName("ORD04 two customers racing for last stock yield one order")
  void race() throws Exception {
    var p = product(1);
    UUID u1 = customer(), u2 = customer();
    orders.quantity(u1, p.id, 1);
    orders.quantity(u2, p.id, 1);
    var r1 = request(u1, "APPROVE");
    var r2 = request(u2, "APPROVE");
    var barrier = new CyclicBarrier(2);
    try (var pool = Executors.newFixedThreadPool(2)) {
      var f1 = pool.submit(() -> racePlace(u1, r1, barrier));
      var f2 = pool.submit(() -> racePlace(u2, r2, barrier));
      assertEquals(1, f1.get(15, TimeUnit.SECONDS) + f2.get(15, TimeUnit.SECONDS));
    }
    assertEquals(0, products.findById(p.id).orElseThrow().stock);
  }

  int racePlace(UUID u, PlaceOrder r, CyclicBarrier b) throws Exception {
    b.await();
    try {
      place(u, UUID.randomUUID().toString(), r);
      return 1;
    } catch (ApiException e) {
      assertEquals(409, e.status);
      return 0;
    }
  }

  @Test
  @DisplayName("SEC02 consumed refresh reuse commits family revocation")
  void reuse() {
    UUID u = customer();
    var first = login(u);
    var second = auth.refresh(first.refreshToken());
    assertNotNull(second.accessToken());
    assertEquals(
        "REFRESH_REUSE",
        assertThrows(ApiException.class, () -> auth.refresh(first.refreshToken())).code);
    assertThrows(ApiException.class, () -> auth.refresh(second.refreshToken()));
    assertTrue(sessions.findByUserId(u).getFirst().revoked);
  }

  @Test
  @DisplayName("SEC02 simultaneous refresh follows strict family revocation policy")
  void refreshRace() throws Exception {
    UUID u = customer();
    var first = login(u);
    var b = new CyclicBarrier(2);
    try (var pool = Executors.newFixedThreadPool(2)) {
      var f1 = pool.submit(() -> raceRefresh(first.refreshToken(), b));
      var f2 = pool.submit(() -> raceRefresh(first.refreshToken(), b));
      assertEquals(1, f1.get(15, TimeUnit.SECONDS) + f2.get(15, TimeUnit.SECONDS));
    }
    assertTrue(sessions.findByUserId(u).getFirst().revoked);
  }

  int raceRefresh(String token, CyclicBarrier b) throws Exception {
    b.await();
    try {
      auth.refresh(token);
      return 1;
    } catch (ApiException e) {
      assertEquals("REFRESH_REUSE", e.code);
      return 0;
    }
  }

  @Test
  @DisplayName("SEC03 logout immediately invalidates existing access JWT")
  void revoke() throws Exception {
    UUID u = customer();
    var login = login(u);
    mvc.perform(get("/api/v1/cart").header("Authorization", "Bearer " + login.accessToken()))
        .andExpect(status().isOk());
    auth.logout(login.refreshToken());
    mvc.perform(get("/api/v1/cart").header("Authorization", "Bearer " + login.accessToken()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("SEC01 customer cannot read another order or mutate catalog")
  void ownership() throws Exception {
    UUID a = customer(), b = customer();
    var p = product(5);
    orders.quantity(a, p.id, 1);
    UUID order = (UUID) place(a, UUID.randomUUID().toString(), request(a, "APPROVE")).get("id");
    var l = login(b);
    mvc.perform(get("/api/v1/orders/" + order).header("Authorization", "Bearer " + l.accessToken()))
        .andExpect(status().isNotFound());
    mvc.perform(get("/api/v1/admin/products").header("Authorization", "Bearer " + l.accessToken()))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("SEC04 authentication requires CSRF and exact origin")
  void csrfOrigin() throws Exception {
    String body = s.encode(new Credentials("nobody@example.com", password));
    mvc.perform(
            post("/api/v1/auth/login")
                .header("Origin", "http://localhost:8080")
                .contentType("application/json")
                .content(body))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/v1/auth/login")
                .with(csrf())
                .header("Origin", "https://evil.example")
                .contentType("application/json")
                .content(body))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("ACC01 registration rejects injected privileged role")
  void injection() throws Exception {
    mvc.perform(
            post("/api/v1/auth/register")
                .with(csrf())
                .header("Origin", "http://localhost:8080")
                .contentType("application/json")
                .content(
                    "{\"email\":\"inject@example.com\",\"password\":\"Test account"
                        + " password\",\"confirmPassword\":\"Test account"
                        + " password\",\"role\":\"ADMIN\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("ACC03 reset is single use and revokes sessions")
  void reset() {
    UUID u = customer();
    var l = login(u);
    String email = users.findById(u).orElseThrow().email;
    auth.recovery(email);
    String body =
        s.db.queryForObject("select body from mail_sink where recipient=?", String.class, email);
    String token = body.substring(body.indexOf("reset=") + 6);
    auth.reset(new Reset(token, "Replacement password 123!"));
    assertThrows(ApiException.class, () -> auth.refresh(l.refreshToken()));
    assertThrows(ApiException.class, () -> auth.reset(new Reset(token, "Another password 123!")));
  }

  @Test
  @DisplayName("AUD01 runtime database role cannot alter audit records")
  void auditImmutable() {
    assertThrows(
        org.springframework.dao.DataAccessException.class,
        () -> s.db.update("delete from audit_event where id=?", UUID.randomUUID()));
  }

  @Test
  @DisplayName("INV01 replenishment dispatch never fabricates received stock")
  void replenishment() {
    UUID u = customer();
    var p = product(101);
    orders.quantity(u, p.id, 2);
    place(u, UUID.randomUUID().toString(), request(u, "APPROVE"));
    assertEquals(99, products.findById(p.id).orElseThrow().stock);
    var r = s.db.queryForMap("select * from replenishment_request where product_id=?", p.id);
    UUID id = (UUID) r.get("id");
    admin.receive(u, new Receipt(id, "receipt-" + id));
    assertEquals(199, products.findById(p.id).orElseThrow().stock);
    admin.receive(u, new Receipt(id, "receipt-" + id));
    assertEquals(199, products.findById(p.id).orElseThrow().stock);
  }

  @Test
  @DisplayName("ACC06 stale profile updates are rejected")
  void profileVersion() {
    UUID u = customer();
    var p = customers.get(u);
    customers.save(u, p);
    assertEquals(409, assertThrows(ApiException.class, () -> customers.save(u, p)).status);
  }

  @Test
  @DisplayName("CAT01 used accessories are rejected")
  void accessory() {
    UUID u = customer();
    var input =
        new ProductInput(
            "TEST-" + UUID.randomUUID(),
            "HP",
            "Mouse",
            "ACCESSORY",
            "USED",
            null,
            null,
            null,
            null,
            null,
            BigDecimal.TEN,
            "Test",
            true,
            0);
    assertThrows(ApiException.class, () -> catalog.save(u, null, input));
  }
}
