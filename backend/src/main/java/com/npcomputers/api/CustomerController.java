package com.npcomputers.api;

import static com.npcomputers.security.PrincipalSupport.user;

import com.npcomputers.api.Contracts.*;
import com.npcomputers.service.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/me/profile")
public class CustomerController {
  private final CustomerService s;

  public CustomerController(CustomerService s) {
    this.s = s;
  }

  @GetMapping
  Profile get() {
    return s.get(user());
  }

  @PutMapping
  Profile put(@Valid @RequestBody Profile p) {
    return s.save(user(), p);
  }
}
