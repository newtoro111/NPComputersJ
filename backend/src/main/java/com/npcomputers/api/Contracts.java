package com.npcomputers.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.*;

public class Contracts {
  public record Credentials(
      @NotBlank @Email @Size(max = 254) String email,
      @NotNull @Size(min = 12, max = 128) String password) {}

  public record Registration(
      @NotBlank @Email @Size(max = 254) String email,
      @NotNull @Size(min = 12, max = 128) String password,
      @NotNull String confirmPassword) {}

  public record Recovery(@NotBlank @Email String email) {}

  public record Reset(
      @NotBlank String token, @NotNull @Size(min = 12, max = 128) String password) {}

  public record Contact(
      @NotBlank @Size(max = 80) String firstName,
      @NotBlank @Size(max = 80) String lastName,
      @NotBlank @Email @Size(max = 254) String email,
      @NotBlank @Pattern(regexp = "[+0-9 ()-]{7,40}") String phone) {}

  public record Address(
      @NotBlank @Size(max = 160) String line1,
      @Size(max = 160) String line2,
      @NotBlank @Size(max = 100) String city,
      @NotBlank String state,
      @NotBlank @Pattern(regexp = "[0-9]{5}(-[0-9]{4})?") String zip,
      @NotBlank @Pattern(regexp = "US") String country) {}

  public record Profile(
      @NotBlank @Pattern(regexp = "INDIVIDUAL|BUSINESS") String kind,
      @Size(max = 160) String businessName,
      @NotNull @Valid Contact primary,
      @Valid Contact alternate,
      @NotNull @Valid Address mailing,
      @NotNull @Valid Address shipping,
      long version) {}

  public record Quantity(@Min(1) @Max(1000000) int quantity) {}

  public record Quote(@NotNull @Valid Address shippingAddress) {}

  public record PlaceOrder(
      @NotNull UUID quoteId,
      @NotBlank String quoteDigest,
      @NotNull @Valid Address shippingAddress,
      @NotBlank @Pattern(regexp = "SIMULATED_CARD|SIMULATED_WALLET") String paymentMethod,
      @NotBlank @Pattern(regexp = "APPROVE|DECLINE") String scenario) {}

  public record ProductInput(
      @NotBlank @Size(max = 80) String sku,
      @NotBlank @Size(max = 80) String manufacturer,
      @NotBlank @Size(max = 120) String model,
      @Pattern(regexp = "PC|LAPTOP|TABLET|ACCESSORY") @NotNull String category,
      @Pattern(regexp = "NEW|USED") @NotNull String condition,
      @Size(max = 120) String cpu,
      @Size(max = 80) String ram,
      @Size(max = 120) String storage,
      @Size(max = 120) String gpu,
      @Size(max = 120) String os,
      @NotNull @DecimalMin("0.01") @DecimalMax("9999999999.99") @Digits(integer = 10, fraction = 2)
          BigDecimal price,
      @NotBlank @Size(max = 2000) String description,
      boolean active,
      long version) {}

  public record Adjustment(
      @NotNull UUID productId,
      int delta,
      @NotBlank @Size(max = 200) String reason,
      @NotBlank @Size(max = 80) String reference) {}

  public record Receipt(@NotNull UUID requestId, @NotBlank @Size(max = 80) String reference) {}

  public record Status(
      @Pattern(regexp = "PROCESSING|SHIPPED") @NotNull String status,
      @Size(max = 160) String tracking,
      long version) {}

  public record Role(@Pattern(regexp = "CUSTOMER|ADMIN|SALES|SUPPORT") @NotNull String role) {}

  public record Enabled(boolean enabled) {}

  public record Inquiry(
      @NotBlank @Email @Size(max = 254) String email,
      @NotBlank @Pattern(regexp = "[+0-9 ()-]{7,40}") String phone,
      @NotNull @Pattern(regexp = "Order Inquiry|Product Inquiry|Support|Cancellation|Other")
          String reason,
      @NotBlank @Size(max = 500) String message) {}
}
