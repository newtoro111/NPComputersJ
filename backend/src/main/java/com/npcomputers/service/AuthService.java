package com.npcomputers.service;

import com.npcomputers.api.*;
import com.npcomputers.api.Contracts.*;
import com.npcomputers.domain.*;
import com.npcomputers.persistence.*;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
  private final UserRepository users;
  private final AuthSessionRepository sessions;
  private final RefreshTokenRepository tokens;
  private final ResetTokenRepository resets;
  private final PasswordEncoder passwords;
  private final JwtEncoder encoder;
  private final Support s;
  private final String issuer, audience;
  private final boolean mail;
  private final String dummy;

  public AuthService(
      UserRepository u,
      AuthSessionRepository a,
      RefreshTokenRepository t,
      ResetTokenRepository r,
      PasswordEncoder p,
      JwtEncoder e,
      Support s,
      @Value("${app.issuer}") String iss,
      @Value("${app.audience}") String aud,
      @Value("${app.mail-sink}") boolean mail) {
    users = u;
    sessions = a;
    tokens = t;
    resets = r;
    passwords = p;
    encoder = e;
    this.s = s;
    issuer = iss;
    audience = aud;
    this.mail = mail;
    dummy = p.encode("dummy-nonexistent-account");
  }

  public record Result(String accessToken, String refreshToken, Map<String, Object> user) {}

  @Transactional
  public Map<String, Object> register(Registration r) {
    if (!r.password().equals(r.confirmPassword()))
      throw new ApiException(400, "PASSWORD_MISMATCH", "Passwords must match.");
    validatePassword(r.password());
    String email = normalize(r.email());
    if (users.findByEmail(email).isPresent())
      throw new ApiException(409, "ACCOUNT_EXISTS", "Account cannot be created with this email.");
    var u = new User();
    u.email = email;
    u.passwordHash = passwords.encode(r.password());
    u.role = "CUSTOMER";
    u.accountNumber = s.db.queryForObject("select nextval('account_number_seq')", Long.class);
    users.saveAndFlush(u);
    s.audit(u.id, "REGISTER", u.id.toString());
    return view(u);
  }

  @Transactional(noRollbackFor = ApiException.class)
  public Result login(Credentials r) {
    var u = users.findByEmail(normalize(r.email())).orElse(null);
    boolean match = passwords.matches(r.password(), u == null ? dummy : u.passwordHash);
    if (u == null || !u.enabled || !match) {
      s.audit(null, "LOGIN_FAILED", "identity");
      throw new ApiException(401, "INVALID_CREDENTIALS", "Email or password is incorrect.");
    }
    u.lastAccess = Instant.now();
    var a = new AuthSession();
    a.userId = u.id;
    a.securityVersion = u.securityVersion;
    a.lastUsed = Instant.now();
    a.expiresAt = Instant.now().plus(Duration.ofDays(30));
    sessions.save(a);
    s.audit(u.id, "LOGIN", a.id.toString());
    return issue(u, a);
  }

  @Transactional(noRollbackFor = ApiException.class)
  public Result refresh(String raw) {
    var token =
        tokens
            .findByDigest(s.digest(raw))
            .orElseThrow(() -> new ApiException(401, "INVALID_REFRESH", "Please sign in again."));
    var a = sessions.lock(token.sessionId).orElseThrow();
    tokens.flush();
    var fresh =
        tokens.findById(token.id).orElseThrow(); // reload managed token after session serialization
    boolean used =
        s.db.queryForObject(
            "select consumed from refresh_token where id=?", Boolean.class, token.id);
    if (used) {
      a.revoked = true;
      s.audit(a.userId, "REFRESH_REUSE", a.id.toString());
      throw new ApiException(401, "REFRESH_REUSE", "Please sign in again.");
    }
    var u = users.findById(a.userId).orElseThrow();
    if (a.revoked
        || !u.enabled
        || a.securityVersion != u.securityVersion
        || a.expiresAt.isBefore(Instant.now())
        || fresh.expiresAt.isBefore(Instant.now()))
      throw new ApiException(401, "INVALID_REFRESH", "Please sign in again.");
    fresh.consumed = true;
    a.lastUsed = Instant.now();
    s.audit(u.id, "REFRESH_ROTATE", a.id.toString());
    return issue(u, a);
  }

  private Result issue(User u, AuthSession a) {
    String raw = s.randomToken();
    var t = new RefreshToken();
    t.sessionId = a.id;
    t.digest = s.digest(raw);
    t.expiresAt = Instant.now().plus(Duration.ofDays(7));
    if (t.expiresAt.isAfter(a.expiresAt)) t.expiresAt = a.expiresAt;
    tokens.save(t);
    Instant now = Instant.now();
    var claims =
        JwtClaimsSet.builder()
            .issuer(issuer)
            .audience(List.of(audience))
            .subject(u.id.toString())
            .issuedAt(now)
            .notBefore(now)
            .expiresAt(now.plusSeconds(300))
            .id(UUID.randomUUID().toString())
            .claim("sid", a.id.toString())
            .claim("role", u.role)
            .build();
    String jwt =
        encoder
            .encode(
                JwtEncoderParameters.from(
                    JwsHeader.with(
                            org.springframework.security.oauth2.jose.jws.SignatureAlgorithm.RS256)
                        .keyId("np-v1")
                        .build(),
                    claims))
            .getTokenValue();
    return new Result(jwt, raw, view(u));
  }

  public Map<String, Object> view(User u) {
    return Map.of(
        "id",
        u.id,
        "email",
        u.email,
        "accountNumber",
        u.accountNumber,
        "role",
        u.role,
        "enabled",
        u.enabled);
  }

  @Transactional
  public void logout(String raw) {
    if (raw == null) return;
    tokens
        .findByDigest(s.digest(raw))
        .ifPresent(
            t -> {
              var a = sessions.lock(t.sessionId).orElseThrow();
              a.revoked = true;
              s.audit(a.userId, "LOGOUT", a.id.toString());
            });
  }

  @Transactional
  public void revokeAll(UUID user) {
    for (var a : sessions.findByUserId(user)) a.revoked = true;
    s.audit(user, "REVOKE_ALL", user.toString());
  }

  @Transactional
  public void recovery(String email) {
    users
        .findByEmail(normalize(email))
        .ifPresent(
            u -> {
              var t = new ResetToken();
              String raw = s.randomToken();
              t.userId = u.id;
              t.digest = s.digest(raw);
              t.expiresAt = Instant.now().plusSeconds(900);
              resets.saveAndFlush(t);
              if (mail)
                s.db.update(
                    "insert into mail_sink(id,recipient,subject,body,created_at)"
                        + " values(?,?,?,?,now())",
                    UUID.randomUUID(),
                    u.email,
                    "Password recovery",
                    "Local demo reset link: /?reset=" + raw);
              s.audit(u.id, "RECOVERY_REQUEST", u.id.toString());
            });
  }

  @Transactional
  public void reset(Reset r) {
    validatePassword(r.password());
    var t =
        resets
            .lockDigest(s.digest(r.token()))
            .orElseThrow(
                () -> new ApiException(400, "INVALID_RESET", "Reset link is invalid or expired."));
    if (t.consumed || t.expiresAt.isBefore(Instant.now()))
      throw new ApiException(400, "INVALID_RESET", "Reset link is invalid or expired.");
    var u = users.lock(t.userId).orElseThrow();
    u.passwordHash = passwords.encode(r.password());
    u.securityVersion++;
    t.consumed = true;
    revokeAll(u.id);
    s.audit(u.id, "PASSWORD_RESET", u.id.toString());
  }

  static String normalize(String v) {
    return v.trim().toLowerCase(Locale.ROOT);
  }

  public static void validatePassword(String v) {
    if (v == null || v.length() < 12 || v.length() > 128)
      throw new ApiException(400, "PASSWORD_LENGTH", "Password must be 12 to 128 characters.");
  }
}
