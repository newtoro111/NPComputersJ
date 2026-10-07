package com.npcomputers.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RequestFilter extends OncePerRequestFilter {
  private final String origin;

  public RequestFilter(@Value("${app.origin}") String origin) {
    this.origin = origin;
  }

  protected void doFilterInternal(HttpServletRequest r, HttpServletResponse s, FilterChain c)
      throws ServletException, IOException {
    String id = r.getHeader("X-Correlation-ID");
    if (id == null || !id.matches("[a-zA-Z0-9-]{1,80}")) id = UUID.randomUUID().toString();
    MDC.put("correlationId", id);
    s.setHeader("X-Correlation-ID", id);
    long start = System.nanoTime();
    try {
      if (r.getRequestURI().startsWith("/api/v1/auth/")
          && !ListHolder.SAFE.contains(r.getMethod())
          && !origin.equals(r.getHeader("Origin"))) {
        s.sendError(403, "Origin not allowed");
        return;
      }
      c.doFilter(r, s);
    } finally {
      LoggerFactory.getLogger(getClass())
          .info(
              "request method={} path={} status={} durationMs={}",
              r.getMethod(),
              r.getRequestURI(),
              s.getStatus(),
              (System.nanoTime() - start) / 1000000);
      MDC.clear();
    }
  }

  private static class ListHolder {
    static final java.util.Set<String> SAFE = java.util.Set.of("GET", "HEAD", "OPTIONS");
  }
}
