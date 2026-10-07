package com.npcomputers.service;

import com.npcomputers.api.ApiException;
import com.npcomputers.api.Contracts.*;
import com.npcomputers.persistence.UserRepository;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {
  private final Support s;
  private final UserRepository users;

  public CustomerService(Support s, UserRepository u) {
    this.s = s;
    users = u;
  }

  public static final Set<String> STATES =
      Set.of(
          "AL", "AK", "AZ", "AR", "CA", "CO", "CT", "DE", "DC", "FL", "GA", "HI", "ID", "IL", "IN",
          "IA", "KS", "KY", "LA", "ME", "MD", "MA", "MI", "MN", "MS", "MO", "MT", "NE", "NV", "NH",
          "NJ", "NM", "NY", "NC", "ND", "OH", "OK", "OR", "PA", "RI", "SC", "SD", "TN", "TX", "UT",
          "VT", "VA", "WA", "WV", "WI", "WY");

  public static void address(Address a) {
    if (a == null || !STATES.contains(a.state()) || !"US".equals(a.country()))
      throw new ApiException(400, "US_ADDRESS", "Select a valid U.S. state and country.");
  }

  public Profile get(UUID id) {
    var rows = s.db.queryForList("select * from customer_profile where user_id=?", id);
    if (rows.isEmpty())
      throw new ApiException(404, "PROFILE_MISSING", "Complete your profile before checkout.");
    var r = rows.getFirst();
    return new Profile(
        (String) r.get("kind"),
        (String) r.get("business_name"),
        contact(id, "P"),
        contact(id, "A"),
        address(id, "M"),
        address(id, "S"),
        ((Number) r.get("version")).longValue());
  }

  private Contact contact(UUID id, String type) {
    return s
        .db
        .query(
            "select * from contact where user_id=? and type=?",
            (r, n) ->
                new Contact(
                    r.getString("first_name"),
                    r.getString("last_name"),
                    r.getString("email"),
                    r.getString("phone")),
            id,
            type)
        .stream()
        .findFirst()
        .orElse(null);
  }

  private Address address(UUID id, String type) {
    return s
        .db
        .query(
            "select * from address where user_id=? and type=?",
            (r, n) ->
                new Address(
                    r.getString("line1"),
                    r.getString("line2"),
                    r.getString("city"),
                    r.getString("state"),
                    r.getString("zip"),
                    r.getString("country")),
            id,
            type)
        .stream()
        .findFirst()
        .orElse(null);
  }

  @Transactional
  public Profile save(UUID id, Profile p) {
    users.lock(id).orElseThrow();
    address(p.mailing());
    address(p.shipping());
    if (p.kind().equals("BUSINESS") && (p.businessName() == null || p.businessName().isBlank()))
      throw new ApiException(400, "BUSINESS_NAME", "Business name is required.");
    var rows = s.db.queryForList("select version from customer_profile where user_id=?", id);
    long version = rows.isEmpty() ? 0 : ((Number) rows.getFirst().get("version")).longValue();
    if (p.version() != version)
      throw new ApiException(409, "PROFILE_CHANGED", "Reload the profile before saving.");
    s.db.update(
        "insert into customer_profile(user_id,kind,business_name,version) values(?,?,?,1) on"
            + " conflict(user_id) do update set"
            + " kind=excluded.kind,business_name=excluded.business_name,version=customer_profile.version+1",
        id,
        p.kind(),
        p.businessName());
    saveContact(id, "P", p.primary());
    saveContact(id, "A", p.alternate());
    saveAddress(id, "M", p.mailing());
    saveAddress(id, "S", p.shipping());
    s.audit(id, "PROFILE_SAVE", id.toString());
    return get(id);
  }

  private void saveContact(UUID id, String type, Contact c) {
    if (c == null) {
      s.db.update("delete from contact where user_id=? and type=?", id, type);
      return;
    }
    s.db.update(
        "insert into"
            + " contact(id,user_id,type,first_name,last_name,email,phone,created_at,updated_at)"
            + " values(?,?,?,?,?,?,?,now(),now()) on conflict(user_id,type) do update set"
            + " first_name=excluded.first_name,last_name=excluded.last_name,email=excluded.email,phone=excluded.phone,updated_at=now()",
        UUID.randomUUID(),
        id,
        type,
        c.firstName(),
        c.lastName(),
        c.email(),
        c.phone());
  }

  private void saveAddress(UUID id, String type, Address a) {
    s.db.update(
        "insert into address(id,user_id,type,line1,line2,city,state,zip,country)"
            + " values(?,?,?,?,?,?,?,?,?) on conflict(user_id,type) do update set"
            + " line1=excluded.line1,line2=excluded.line2,city=excluded.city,state=excluded.state,zip=excluded.zip,country=excluded.country",
        UUID.randomUUID(),
        id,
        type,
        a.line1(),
        a.line2(),
        a.city(),
        a.state(),
        a.zip(),
        a.country());
  }
}
