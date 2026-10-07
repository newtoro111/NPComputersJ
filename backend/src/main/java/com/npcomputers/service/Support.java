package com.npcomputers.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import org.slf4j.MDC;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class Support {
  public final JdbcTemplate db;
  public final ObjectMapper json;

  public Support(JdbcTemplate db, ObjectMapper json) {
    this.db = db;
    this.json = json;
  }

  public String encode(Object value) {
    try {
      return json.writeValueAsString(value);
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid JSON", e);
    }
  }

  public <T> T decode(String value, Class<T> type) {
    try {
      return json.readValue(value, type);
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid JSON", e);
    }
  }

  public String digest(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  public String randomToken() {
    byte[] b = new byte[32];
    new java.security.SecureRandom().nextBytes(b);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
  }

  public void audit(UUID actor, String action, String entity) {
    db.update(
        "insert into audit_event(id,actor,action,entity,correlation_id,created_at)"
            + " values(?,?,?,?,?,now())",
        UUID.randomUUID(),
        actor,
        action,
        entity,
        MDC.get("correlationId"));
  }

  public void outbox(String kind, String payload) {
    db.update(
        "insert into outbox(id,kind,payload,created_at) values(?,?,?,now())",
        UUID.randomUUID(),
        kind,
        payload);
  }
}
