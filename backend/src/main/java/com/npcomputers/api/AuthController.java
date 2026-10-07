package com.npcomputers.api;

import com.npcomputers.api.Contracts.*;
import com.npcomputers.security.PrincipalSupport;
import com.npcomputers.service.*;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
  private final AuthService auth;
  private final RateLimiter rates;
  private final boolean secure;

  public AuthController(AuthService a, RateLimiter r, @Value("${app.secure-cookie}") boolean sec) {
    auth = a;
    rates = r;
    secure = sec;
  }

  @GetMapping("/csrf")
  Map<String, Object> csrf(CsrfToken t) {
    return Map.of("token", t.getToken());
  }

  @PostMapping("/register")
  ResponseEntity<?> register(@Valid @RequestBody Registration r, HttpServletRequest req) {
    rates.hit("register:" + req.getRemoteAddr(), 20, 3600);
    return ResponseEntity.status(201).body(auth.register(r));
  }

  @PostMapping("/login")
  Object login(@Valid @RequestBody Credentials r, HttpServletRequest req, HttpServletResponse res) {
    String key =
        "login:" + r.email().trim().toLowerCase(java.util.Locale.ROOT) + ":" + req.getRemoteAddr();
    rates.hit(key, 5, 900);
    var result = auth.login(r);
    rates.clear(key);
    return respond(result, res);
  }

  @PostMapping("/refresh")
  Object refresh(
      @CookieValue(name = "NP_REFRESH", defaultValue = "") String token, HttpServletResponse res) {
    return respond(auth.refresh(token), res);
  }

  @PostMapping("/logout")
  ResponseEntity<?> logout(
      @CookieValue(name = "NP_REFRESH", required = false) String token, HttpServletResponse res) {
    auth.logout(token);
    cookie(res, "", 0);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/logout-all")
  @PreAuthorize("isAuthenticated()")
  ResponseEntity<?> all(HttpServletResponse res) {
    auth.revokeAll(PrincipalSupport.user());
    cookie(res, "", 0);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/recovery")
  ResponseEntity<?> recovery(@Valid @RequestBody Recovery r, HttpServletRequest req) {
    rates.hit("recovery:" + req.getRemoteAddr(), 5, 3600);
    auth.recovery(r.email());
    return ResponseEntity.accepted()
        .body(
            Map.of(
                "message",
                "If the account exists, recovery instructions were generated. Email delivery is"
                    + " simulated."));
  }

  @PostMapping("/reset")
  ResponseEntity<?> reset(@Valid @RequestBody Reset r, HttpServletResponse res) {
    auth.reset(r);
    cookie(res, "", 0);
    return ResponseEntity.noContent().build();
  }

  private Object respond(AuthService.Result r, HttpServletResponse res) {
    cookie(res, r.refreshToken(), 604800);
    return Map.of("accessToken", r.accessToken(), "expiresIn", 300, "user", r.user());
  }

  private void cookie(HttpServletResponse res, String value, long age) {
    res.addHeader(
        "Set-Cookie",
        ResponseCookie.from("NP_REFRESH", value)
            .path("/api/v1/auth")
            .httpOnly(true)
            .secure(secure)
            .sameSite("Strict")
            .maxAge(age)
            .build()
            .toString());
  }
}
