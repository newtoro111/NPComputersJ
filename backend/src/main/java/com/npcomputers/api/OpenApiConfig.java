package com.npcomputers.api;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.*;
import org.springframework.context.annotation.*;

@Configuration
public class OpenApiConfig {
  @Bean
  OpenAPI api() {
    return new OpenAPI()
        .info(
            new Info()
                .title("NP Computers API")
                .version("1.0.0")
                .description(
                    "U.S. storefront with simulated payments only. Auth writes require same-origin"
                        + " CSRF token from /api/v1/auth/csrf. Order submission requires"
                        + " Idempotency-Key; declines return PAYMENT_FAILED. Money is USD. Errors"
                        + " use application/problem+json."))
        .components(
            new Components()
                .addSecuritySchemes(
                    "bearerAuth",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
        .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
  }
}
