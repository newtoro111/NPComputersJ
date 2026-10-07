package com.npcomputers.security;

import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.npcomputers.persistence.*;
import java.nio.file.*;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.*;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.*;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
  @Bean
  PasswordEncoder passwords() {
    return org.springframework.security.crypto.password.Pbkdf2PasswordEncoder
        .defaultsForSpringSecurity_v5_8();
  }

  @Bean
  RSAKey rsa(@Value("${app.private-key}") String priv, @Value("${app.public-key}") String pub)
      throws Exception {
    var f = KeyFactory.getInstance("RSA");
    return new RSAKey.Builder((RSAPublicKey) f.generatePublic(new X509EncodedKeySpec(pem(pub))))
        .privateKey((RSAPrivateKey) f.generatePrivate(new PKCS8EncodedKeySpec(pem(priv))))
        .keyID("np-v1")
        .build();
  }

  byte[] pem(String file) throws Exception {
    return Base64.getMimeDecoder()
        .decode(Files.readString(Path.of(file)).replaceAll("-----[^-]+-----", ""));
  }

  @Bean
  JwtEncoder encoder(RSAKey key) {
    return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)));
  }

  @Bean
  JwtDecoder decoder(
      RSAKey key,
      @Value("${app.issuer}") String issuer,
      @Value("${app.audience}") String audience,
      AuthSessionRepository sessions,
      UserRepository users)
      throws Exception {
    var d = NimbusJwtDecoder.withPublicKey(key.toRSAPublicKey()).build();
    d.setJwtValidator(
        new DelegatingOAuth2TokenValidator<>(
            new JwtTimestampValidator(java.time.Duration.ofSeconds(30)),
            new JwtIssuerValidator(issuer),
            jwt -> {
              try {
                var s =
                    sessions.findById(UUID.fromString(jwt.getClaimAsString("sid"))).orElseThrow();
                var u = users.findById(s.userId).orElseThrow();
                if (jwt.getAudience().contains(audience)
                    && jwt.getSubject().equals(u.id.toString())
                    && !s.revoked
                    && s.expiresAt.isAfter(Instant.now())
                    && u.enabled
                    && u.securityVersion == s.securityVersion)
                  return OAuth2TokenValidatorResult.success();
              } catch (Exception ignored) {
              }
              return OAuth2TokenValidatorResult.failure(
                  new OAuth2Error("invalid_token", "Session invalid", null));
            }));
    return d;
  }

  @Bean
  SecurityFilterChain chain(HttpSecurity http, @Value("${app.secure-cookie}") boolean secure)
      throws Exception {
    var csrf = new CookieCsrfTokenRepository();
    csrf.setCookieCustomizer(c -> c.path("/").secure(secure).sameSite("Strict").httpOnly(false));
    var handler = new CsrfTokenRequestAttributeHandler();
    handler.setCsrfRequestAttributeName(null);
    http.csrf(
    c ->
        c.csrfTokenRepository(csrf)
            .csrfTokenRequestHandler(handler)
            .ignoringRequestMatchers(
                "/api/v1/auth/register",
                "/api/v1/auth/login"
            )
)
        .sessionManagement(
            s ->
                s.sessionCreationPolicy(
                    org.springframework.security.config.http.SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            a ->
                a.requestMatchers(
                        "/api/v1/auth/**",
                        "/api/v1/products/**",
                        "/api/v1/contact-inquiries",
                        "/actuator/health/**",
                        "/v3/api-docs/**",
                        "/swagger-ui/**")
                    .permitAll()
                    .requestMatchers("/actuator/**")
                    .hasRole("ADMIN")
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(
            o ->
                o.jwt(
                    j ->
                        j.jwtAuthenticationConverter(
                            jwt -> {
                              return new JwtAuthenticationToken(
                                  jwt,
                                  List.of(
                                      new SimpleGrantedAuthority(
                                          "ROLE_" + jwt.getClaimAsString("role"))));
                            })))
        .headers(
            h ->
                h.contentSecurityPolicy(
                    c ->
                        c.policyDirectives(
                            "default-src 'self'; frame-ancestors 'none'; object-src 'none'")));
    return http.build();
  }
}
