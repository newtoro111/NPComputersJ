package com.npcomputers.service;

import com.npcomputers.api.*;
import com.npcomputers.api.Contracts.*;
import com.npcomputers.persistence.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {
  private final Support s;
  private final UserRepository users;
  private final ProductRepository products;
  private final AuthService auth;

  public AdminService(Support s, UserRepository u, ProductRepository p, AuthService a) {
    this.s = s;
    users = u;
    products = p;
    auth = a;
  }

  @Transactional
  public void role(UUID actor, UUID id, String role) {
    var u =
        users.lock(id).orElseThrow(() -> new ApiException(404, "USER_MISSING", "User not found."));
    if (id.equals(actor))
      throw new ApiException(400, "SELF_CHANGE", "Cannot change your own role.");
    u.role = role;
    u.securityVersion++;
    auth.revokeAll(id);
    s.audit(actor, "ROLE_CHANGE", id.toString());
  }

  @Transactional
  public void enabled(UUID actor, UUID id, boolean enabled) {
    var u =
        users.lock(id).orElseThrow(() -> new ApiException(404, "USER_MISSING", "User not found."));
    if (id.equals(actor))
      throw new ApiException(400, "SELF_CHANGE", "Cannot disable your own account.");
    u.enabled = enabled;
    u.securityVersion++;
    auth.revokeAll(id);
    s.audit(actor, "ACCOUNT_ENABLED_CHANGE", id.toString());
  }

  @Transactional
  public Object adjust(UUID actor, Adjustment r) {
    var p =
        products
            .lock(r.productId())
            .orElseThrow(() -> new ApiException(404, "PRODUCT_MISSING", "Product not found."));
    var old =
        s.db.queryForList(
            "select delta,reason from stock_movement where product_id=? and reference=?",
            p.id,
            r.reference());
    if (!old.isEmpty()) {
      if (((Number) old.getFirst().get("delta")).intValue() != r.delta()
          || !old.getFirst().get("reason").equals(r.reason()))
        throw new ApiException(
            409, "REFERENCE_REUSED", "Reference already used for another adjustment.");
      return Map.of("stock", p.stock);
    }
    if ((long) p.stock + r.delta() < 0 || (long) p.stock + r.delta() > 1000000)
      throw new ApiException(400, "STOCK_RANGE", "Stock must be between zero and one million.");
    p.stock += r.delta();
    s.db.update(
        "insert into stock_movement(id,product_id,reference,delta,reason,created_at)"
            + " values(?,?,?,?,?,now())",
        UUID.randomUUID(),
        p.id,
        r.reference(),
        r.delta(),
        r.reason());
    s.audit(actor, "STOCK_ADJUST", p.id.toString());
    return Map.of("stock", p.stock);
  }

  @Transactional
  public Object receive(UUID actor, Receipt r) {
    var rows = s.db.queryForList("select * from replenishment_request where id=?", r.requestId());
    if (rows.isEmpty()) throw new ApiException(404, "REQUEST_MISSING", "Request not found.");
    var request = rows.getFirst();
    UUID product = (UUID) request.get("product_id");
    products.lock(product).orElseThrow();
    request =
        s.db.queryForMap(
            "select * from replenishment_request where id=? for update", r.requestId());
    if (request.get("status").equals("RECEIVED")) return Map.of("status", "RECEIVED");
    var result =
        adjust(
            actor,
            new Adjustment(
                product,
                ((Number) request.get("quantity")).intValue(),
                "Replenishment receipt " + r.requestId(),
                r.reference()));
    s.db.update(
        "update replenishment_request set status='RECEIVED',received_at=now() where id=?",
        r.requestId());
    return result;
  }

  public Object users(int page) {
    return s.db.queryForList(
        "select id,email,account_number,role,enabled from app_user order by account_number limit 20"
            + " offset ?",
        Math.max(0, page) * 20);
  }

  public Object audit(int page) {
    return s.db.queryForList(
        "select * from audit_event order by created_at desc limit 50 offset ?",
        Math.max(0, page) * 50);
  }

  public Object replenishment() {
    return s.db.queryForList(
        "select r.*,p.sku from replenishment_request r join product p on p.id=r.product_id order by"
            + " r.created_at desc limit 100");
  }
}
