package com.npcomputers.service;

import com.npcomputers.api.*;
import com.npcomputers.api.Contracts.*;
import com.npcomputers.domain.*;
import com.npcomputers.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
  private final UserRepository users;
  private final ProductRepository products;
  private final CartLineRepository carts;
  private final CustomerOrderRepository orders;
  private final OrderLineRepository lines;
  private final PaymentAttemptRepository payments;
  private final PaymentService simulator;
  private final CustomerService customers;
  private final CatalogService catalog;
  private final Support s;

  public OrderService(
      UserRepository u,
      ProductRepository p,
      CartLineRepository c,
      CustomerOrderRepository o,
      OrderLineRepository l,
      PaymentAttemptRepository pay,
      PaymentService sim,
      CustomerService cs,
      CatalogService cat,
      Support s) {
    users = u;
    products = p;
    carts = c;
    orders = o;
    lines = l;
    payments = pay;
    simulator = sim;
    customers = cs;
    catalog = cat;
    this.s = s;
  }

  public record Item(
      UUID productId,
      String sku,
      String description,
      String condition,
      int quantity,
      BigDecimal unitPrice) {}

  public record Snapshot(
      List<Item> items,
      Address shippingAddress,
      BigDecimal subtotal,
      BigDecimal shipping,
      BigDecimal tax,
      BigDecimal total,
      String policy) {}

  public Object cart(UUID user) {
    return carts.findByUserIdOrderByProductId(user).stream()
        .map(
            c -> {
              var p = products.findById(c.productId).orElseThrow();
              return Map.of("product", catalog.view(p), "quantity", c.quantity);
            })
        .toList();
  }

  @Transactional
  public Object quantity(UUID user, UUID product, int qty) {
    users.lock(user).orElseThrow();
    var p = catalog.active(product);
    if (qty > p.stock)
      throw new ApiException(409, "STOCK_LOW", "Quantity exceeds available stock.");
    var c = carts.findByUserIdAndProductId(user, product).orElseGet(CartLine::new);
    c.userId = user;
    c.productId = product;
    c.quantity = qty;
    carts.save(c);
    return cart(user);
  }

  @Transactional
  public void remove(UUID user, UUID product) {
    users.lock(user).orElseThrow();
    carts.findByUserIdAndProductId(user, product).ifPresent(carts::delete);
  }

  @Transactional
  public void clear(UUID user) {
    users.lock(user).orElseThrow();
    carts.deleteByUserId(user);
  }

  Snapshot snapshot(UUID user, Address address, boolean lock) {
    CustomerService.address(address);
    var cart = carts.findByUserIdOrderByProductId(user);
    if (cart.isEmpty()) throw new ApiException(409, "EMPTY_CART", "Add items to your cart first.");
    List<Item> items = new ArrayList<>();
    BigDecimal subtotal = BigDecimal.ZERO;
    for (var c : cart) {
      var p =
          lock
              ? products.lock(c.productId).orElseThrow()
              : products.findById(c.productId).orElseThrow();
      if (!p.active || c.quantity > p.stock)
        throw new ApiException(
            409, "STOCK_CHANGED", "Stock or product availability changed. Update your cart.");
      items.add(new Item(p.id, p.sku, p.description, p.condition, c.quantity, p.price));
      subtotal = subtotal.add(p.price.multiply(BigDecimal.valueOf(c.quantity)));
    }
    BigDecimal shipping = new BigDecimal("10.00"), tax = new BigDecimal("0.00");
    return new Snapshot(
        items,
        address,
        subtotal.setScale(2),
        shipping,
        tax,
        subtotal.add(shipping).setScale(2),
        "DEMO_FLAT_10_TAX_ZERO_V1");
  }

  @Transactional
  public Object quote(UUID user, Quote q) {
    users.lock(user).orElseThrow();
    customers.get(user);
    var snap = snapshot(user, q.shippingAddress(), false);
    UUID id = UUID.randomUUID();
    String data = s.encode(snap), digest = s.digest(data);
    Instant expiry = Instant.now().plusSeconds(600);
    s.db.update(
        "insert into checkout_quote(id,user_id,digest,snapshot,expires_at) values(?,?,?,?,?)",
        id,
        user,
        digest,
        data,
        java.sql.Timestamp.from(expiry));
    return Map.of("id", id, "digest", digest, "expiresAt", expiry, "quote", snap);
  }

  @Transactional
  public Object place(UUID user, String key, PlaceOrder r) {
    if (key == null || !key.matches("[a-zA-Z0-9-]{8,100}"))
      throw new ApiException(400, "IDEMPOTENCY_KEY", "Provide a valid Idempotency-Key.");
    users.lock(user).orElseThrow();
    String hash = s.digest(s.encode(r));
    var previous =
        s.db.queryForList(
            "select digest,order_id from idempotency_record where user_id=? and key=?", user, key);
    if (!previous.isEmpty()) {
      var old = previous.getFirst();
      if (!hash.equals(old.get("digest")))
        throw new ApiException(409, "KEY_REUSED", "Use a new key for a different submission.");
      return detail(user, (UUID) old.get("order_id"), false);
    }
    var qr =
        s.db.queryForList(
            "select * from checkout_quote where id=? and user_id=?", r.quoteId(), user);
    if (qr.isEmpty()) throw new ApiException(409, "QUOTE_INVALID", "Review a new quote.");
    var q = qr.getFirst();
    if (((java.sql.Timestamp) q.get("expires_at")).toInstant().isBefore(Instant.now())
        || !r.quoteDigest().equals(q.get("digest")))
      throw new ApiException(409, "QUOTE_EXPIRED", "Review a new quote.");
    var snap = snapshot(user, r.shippingAddress(), true);
    if (!s.digest(s.encode(snap)).equals(r.quoteDigest()))
      throw new ApiException(
          409, "QUOTE_CHANGED", "Your quote changed. Review a new quote before paying.");
    var profile = customers.get(user);
    var o = new CustomerOrder();
    o.userId = user;
    o.orderNumber =
        "NPC-"
            + o.id.toString().substring(0, 8).toUpperCase()
            + "-"
            + o.id.toString().substring(9, 13).toUpperCase();
    o.subtotal = snap.subtotal();
    o.shipping = snap.shipping();
    o.tax = snap.tax();
    o.total = snap.total();
    o.shippingSnapshot = s.encode(snap.shippingAddress());
    o.contactSnapshot = s.encode(profile.primary());
    var result = simulator.pay(o.id, o.total, r.paymentMethod(), r.scenario());
    o.status = result.approved() ? "CONFIRMED" : "PAYMENT_FAILED";
    orders.saveAndFlush(o);
    for (var i : snap.items()) {
      var l = new OrderLine();
      l.orderId = o.id;
      l.productId = i.productId();
      l.sku = i.sku();
      l.description = i.description();
      l.condition = i.condition();
      l.quantity = i.quantity();
      l.unitPrice = i.unitPrice();
      lines.save(l);
      if (result.approved()) {
        var p = products.findById(i.productId()).orElseThrow();
        p.stock -= i.quantity();
        s.audit(user, "STOCK_DECREMENT", p.id.toString());
        s.db.update(
            "insert into stock_movement(id,product_id,reference,delta,reason,created_at)"
                + " values(?,?,?,?,?,now())",
            UUID.randomUUID(),
            p.id,
            o.id.toString(),
            -i.quantity(),
            "Approved order");
        if (p.stock <= p.reorderPoint) {
          UUID request = UUID.randomUUID();
          int inserted =
              s.db.update(
                  "insert into replenishment_request(id,product_id,quantity,status,created_at)"
                      + " values(?,?,?,'PENDING',now()) on conflict(product_id) where"
                      + " status<>'RECEIVED' do nothing",
                  request,
                  p.id,
                  p.reorderQuantity);
          if (inserted == 1) s.outbox("REPLENISHMENT", request.toString());
        }
      }
    }
    var pay = new PaymentAttempt();
    pay.orderId = o.id;
    pay.method = r.paymentMethod();
    pay.status = result.approved() ? "APPROVED" : "DECLINED";
    pay.amount = o.total;
    pay.transactionRef = result.reference();
    payments.saveAndFlush(pay);
    if (result.approved()) {
      carts.deleteByUserId(user);
      s.outbox("CONFIRMATION", o.id.toString());
    }
    s.db.update(
        "insert into idempotency_record(user_id,key,digest,order_id) values(?,?,?,?)",
        user,
        key,
        hash,
        o.id);
    s.audit(user, "ORDER_" + o.status, o.id.toString());
    s.audit(user, "PAYMENT_" + pay.status, pay.id.toString());
    products.flush();
    lines.flush();
    return detail(user, o.id, false);
  }

  public Object list(UUID user, boolean staff, int page) {
    var pg = PageRequest.of(Math.max(0, page), 20, Sort.by("createdAt").descending());
    var data = staff ? orders.findAll(pg) : orders.findByUserIdOrderByCreatedAtDesc(user, pg);
    return Map.of(
        "items",
        data.getContent().stream().map(this::summary).toList(),
        "total",
        data.getTotalElements(),
        "page",
        page);
  }

  Map<String, Object> summary(CustomerOrder o) {
    return Map.of(
        "id",
        o.id,
        "orderNumber",
        o.orderNumber,
        "status",
        o.status,
        "total",
        o.total,
        "createdAt",
        o.createdAt,
        "version",
        o.version);
  }

  public Object detail(UUID user, UUID id, boolean staff) {
    var o =
        orders
            .findById(id)
            .orElseThrow(() -> new ApiException(404, "ORDER_MISSING", "Order not found."));
    if (!staff && !o.userId.equals(user))
      throw new ApiException(404, "ORDER_MISSING", "Order not found.");
    var v = new LinkedHashMap<String, Object>(summary(o));
    v.put("subtotal", o.subtotal);
    v.put("shipping", o.shipping);
    v.put("tax", o.tax);
    v.put("shippingAddress", s.decode(o.shippingSnapshot, Address.class));
    v.put("primaryContact", s.decode(o.contactSnapshot, Contact.class));
    v.put(
        "items",
        lines.findByOrderId(id).stream()
            .map(
                l ->
                    new Item(
                        l.productId, l.sku, l.description, l.condition, l.quantity, l.unitPrice))
            .toList());
    var pay = payments.findByOrderId(id).orElseThrow();
    v.put(
        "payment",
        Map.of("method", pay.method, "status", pay.status, "reference", pay.transactionRef));
    v.put("tracking", o.tracking);
    v.put("notification", "Email delivery is simulated. No real payment was collected.");
    return v;
  }

  @Transactional
  public Object status(UUID actor, UUID id, Status r) {
    var o =
        orders
            .findById(id)
            .orElseThrow(() -> new ApiException(404, "ORDER_MISSING", "Order not found."));
    if (o.version != r.version())
      throw new ApiException(409, "ORDER_CHANGED", "Reload order before changing status.");
    if (!(o.status.equals("CONFIRMED") && r.status().equals("PROCESSING")
        || o.status.equals("PROCESSING") && r.status().equals("SHIPPED")))
      throw new ApiException(409, "INVALID_TRANSITION", "Invalid order transition.");
    if (r.status().equals("SHIPPED") && (r.tracking() == null || r.tracking().isBlank()))
      throw new ApiException(400, "TRACKING_REQUIRED", "Tracking reference is required.");
    o.status = r.status();
    o.tracking = r.tracking();
    orders.saveAndFlush(o);
    s.audit(actor, "FULFILLMENT_" + o.status, o.id.toString());
    return detail(actor, id, true);
  }
}
