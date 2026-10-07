package com.npcomputers.security;

import java.util.UUID;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public class PrincipalSupport {
  public static UUID user() {
    return UUID.fromString(
        ((JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication())
            .getToken()
            .getSubject());
  }

  public static UUID session() {
    return UUID.fromString(
        ((JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication())
            .getToken()
            .getClaimAsString("sid"));
  }

  public static boolean staff() {
    return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
        .anyMatch(a -> !a.getAuthority().equals("ROLE_CUSTOMER"));
  }
}
