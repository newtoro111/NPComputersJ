package com.npcomputers.api;

import com.npcomputers.service.CatalogService;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/products")
public class CatalogController {
  private final CatalogService s;

  public CatalogController(CatalogService s) {
    this.s = s;
  }

  @GetMapping
  Object list(
      @RequestParam(defaultValue = "") String q,
      @RequestParam(defaultValue = "") String category,
      @RequestParam(defaultValue = "") String manufacturer,
      @RequestParam(defaultValue = "") String condition,
      @RequestParam(defaultValue = "0") BigDecimal min,
      @RequestParam(defaultValue = "9999999999") BigDecimal max,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return s.list(q, category, manufacturer, condition, min, max, page, size, false);
  }

  @GetMapping("/{id}")
  Object detail(@PathVariable UUID id) {
    return s.view(s.active(id));
  }
}
