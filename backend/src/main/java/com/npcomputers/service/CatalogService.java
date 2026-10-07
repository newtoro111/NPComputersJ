package com.npcomputers.service;

import com.npcomputers.api.*;
import com.npcomputers.api.Contracts.*;
import com.npcomputers.domain.Product;
import com.npcomputers.persistence.ProductRepository;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogService {
  private final ProductRepository products;
  private final Support s;

  public CatalogService(ProductRepository p, Support s) {
    products = p;
    this.s = s;
  }

  public Map<String, Object> list(
      String q,
      String category,
      String manufacturer,
      String condition,
      java.math.BigDecimal min,
      java.math.BigDecimal max,
      int page,
      int size,
      boolean all) {
    size = Math.max(1, Math.min(size, 100));
    page = Math.max(0, page);
    String where =
        " where (? or active) and (lower(sku||' '||model||' '||description) like ?) and (?='' or"
            + " category=?) and (?='' or manufacturer=?) and (?='' or condition=?) and price>=? and"
            + " price<=?";
    Object[] args = {
      all,
      "%" + q.toLowerCase(Locale.ROOT) + "%",
      category,
      category,
      manufacturer,
      manufacturer,
      condition,
      condition,
      min,
      max
    };
    var count = s.db.queryForObject("select count(*) from product" + where, Long.class, args);
    var a = new ArrayList<>(List.of(args));
    a.add(size);
    a.add(page * size);
    var ids =
        s.db.query(
            "select id from product" + where + " order by manufacturer,model,id limit ? offset ?",
            (r, n) -> r.getObject(1, UUID.class),
            a.toArray());
    return Map.of(
        "items",
        ids.stream().map(id -> view(products.findById(id).orElseThrow())).toList(),
        "page",
        page,
        "size",
        size,
        "total",
        count);
  }

  public Product active(UUID id) {
    var p =
        products
            .findById(id)
            .orElseThrow(() -> new ApiException(404, "PRODUCT_MISSING", "Product not found."));
    if (!p.active) throw new ApiException(404, "PRODUCT_MISSING", "Product not found.");
    return p;
  }

  public Map<String, Object> view(Product p) {
    var v = new LinkedHashMap<String, Object>();
    v.put("id", p.id);
    v.put("sku", p.sku);
    v.put("manufacturer", p.manufacturer);
    v.put("model", p.model);
    v.put("category", p.category);
    v.put("condition", p.condition);
    v.put("cpu", p.cpu);
    v.put("ram", p.ram);
    v.put("storage", p.storage);
    v.put("gpu", p.gpu);
    v.put("os", p.os);
    v.put("price", p.price);
    v.put("stock", p.stock);
    v.put("active", p.active);
    v.put("description", p.description);
    v.put("version", p.version);
    return v;
  }

  @Transactional
  public Map<String, Object> save(UUID actor, UUID id, ProductInput r) {
    if (r.category().equals("ACCESSORY") && !r.condition().equals("NEW"))
      throw new ApiException(400, "ACCESSORY_CONDITION", "Accessories must be new.");
    if (!r.category().equals("ACCESSORY")
        && (blank(r.cpu()) || blank(r.ram()) || blank(r.storage()) || blank(r.os())))
      throw new ApiException(
          400, "SPECS_REQUIRED", "Computer CPU, RAM, storage and OS are required.");
    var p =
        id == null
            ? new Product()
            : products
                .lock(id)
                .orElseThrow(() -> new ApiException(404, "PRODUCT_MISSING", "Product not found."));
    if (id != null && p.version != r.version())
      throw new ApiException(409, "PRODUCT_CHANGED", "Product changed. Reload first.");
    p.sku = r.sku();
    p.manufacturer = r.manufacturer();
    p.model = r.model();
    p.category = r.category();
    p.condition = r.condition();
    p.cpu = r.cpu();
    p.ram = r.ram();
    p.storage = r.storage();
    p.gpu = r.gpu();
    p.os = r.os();
    p.price = r.price();
    p.description = r.description();
    p.active = r.active();
    products.saveAndFlush(p);
    s.audit(actor, "PRODUCT_SAVE", p.id.toString());
    return view(p);
  }

  private boolean blank(String v) {
    return v == null || v.isBlank();
  }
}
