package com.npcomputers.api;

import com.npcomputers.api.Contracts.*;
import com.npcomputers.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
public class ContactController {
  private final ContactService s;
  private final RateLimiter rate;

  public ContactController(ContactService s, RateLimiter r) {
    this.s = s;
    rate = r;
  }

  @PostMapping("/api/v1/contact-inquiries")
  ResponseEntity<?> post(@Valid @RequestBody Inquiry r, HttpServletRequest req) {
    rate.hit("contact:" + req.getRemoteAddr(), 10, 3600);
    return ResponseEntity.accepted()
        .body(
            Map.of(
                "reference",
                s.submit(r),
                "message",
                "Request saved. This demo sends no external email and creates no support ticket."));
  }
}
