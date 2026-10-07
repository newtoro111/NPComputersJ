package com.npcomputers.service;

import com.npcomputers.domain.*;
import com.npcomputers.persistence.*;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class Seeder implements ApplicationRunner {
  private final ProductRepository products;
  private final UserRepository users;
  private final PasswordEncoder passwords;
  private final Support s;
  private final boolean seed;
  private final String email, password;

  public Seeder(
      ProductRepository p,
      UserRepository u,
      PasswordEncoder pw,
      Support s,
      @Value("${app.seed}") boolean seed,
      @Value("${app.bootstrap-email}") String email,
      @Value("${app.bootstrap-password}") String pass) {
    products = p;
    users = u;
    passwords = pw;
    this.s = s;
    this.seed = seed;
    this.email = email;
    password = pass;
  }

  @Transactional
  public void run(ApplicationArguments args) {
    if (!email.isBlank()
        && !password.isBlank()
        && users.findByEmail(email.toLowerCase()).isEmpty()) {
      AuthService.validatePassword(password);
      var u = new User();
      u.email = email.toLowerCase();
      u.passwordHash = passwords.encode(password);
      u.role = "ADMIN";
      u.accountNumber = s.db.queryForObject("select nextval('account_number_seq')", Long.class);
      users.saveAndFlush(u);
      s.audit(u.id, "ADMIN_BOOTSTRAP", u.id.toString());
    }
    if (!seed) return;
    String[][] data = {
      {
        "Apple",
        "MacBook Air demo",
        "LAPTOP",
        "999.00",
        "Apple M-series",
        "16 GB",
        "512 GB SSD",
        "Integrated",
        "macOS"
      },
      {
        "Apple",
        "iMac demo",
        "PC",
        "1299.00",
        "Apple M-series",
        "16 GB",
        "512 GB SSD",
        "Integrated",
        "macOS"
      },
      {
        "Apple",
        "iPad demo",
        "TABLET",
        "499.00",
        "Apple mobile",
        "8 GB",
        "128 GB",
        "Integrated",
        "iPadOS"
      },
      {"Apple", "Keyboard demo", "ACCESSORY", "99.00", null, null, null, null, null},
      {
        "Lenovo",
        "ThinkPad demo",
        "LAPTOP",
        "1099.00",
        "Intel Core",
        "16 GB",
        "1 TB SSD",
        "Integrated",
        "Windows 11"
      },
      {
        "Lenovo",
        "ThinkCentre demo",
        "PC",
        "749.00",
        "Intel Core",
        "16 GB",
        "512 GB SSD",
        "Integrated",
        "Windows 11"
      },
      {
        "Lenovo",
        "Tab demo",
        "TABLET",
        "249.00",
        "Mobile processor",
        "8 GB",
        "128 GB",
        "Integrated",
        "Android"
      },
      {"Lenovo", "USB C dock demo", "ACCESSORY", "129.00", null, null, null, null, null},
      {
        "HP",
        "EliteBook demo",
        "LAPTOP",
        "899.00",
        "AMD Ryzen",
        "16 GB",
        "512 GB SSD",
        "Integrated",
        "Windows 11"
      },
      {
        "HP",
        "ProDesk demo",
        "PC",
        "649.00",
        "Intel Core",
        "16 GB",
        "512 GB SSD",
        "Integrated",
        "Windows 11"
      },
      {
        "HP",
        "Tablet demo",
        "TABLET",
        "399.00",
        "Mobile processor",
        "8 GB",
        "128 GB",
        "Integrated",
        "Windows"
      },
      {"HP", "Wireless mouse demo", "ACCESSORY", "29.00", null, null, null, null, null}
    };
    for (int i = 0; i < data.length; i++) {
      String sku = "DEMO-" + (1000 + i);
      if (products.findBySku(sku).isPresent()) continue;
      var a = data[i];
      var p = new Product();
      p.sku = sku;
      p.manufacturer = a[0];
      p.model = a[1];
      p.category = a[2];
      p.price = new BigDecimal(a[3]);
      p.cpu = a[4];
      p.ram = a[5];
      p.storage = a[6];
      p.gpu = a[7];
      p.os = a[8];
      p.condition = i == 4 ? "USED" : "NEW";
      p.stock = 1000;
      p.description =
          "Illustrative "
              + a[0]
              + " "
              + a[2].toLowerCase()
              + " for the NP Computers demo. Specifications and price are fictional.";
      products.save(p);
    }
  }
}
