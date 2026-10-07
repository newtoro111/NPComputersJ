package com.npcomputers.service;

import com.npcomputers.api.Contracts.Inquiry;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContactService {
  private final Support s;

  public ContactService(Support s) {
    this.s = s;
  }

  @Transactional
  public UUID submit(Inquiry r) {
    UUID id = UUID.randomUUID();
    s.db.update(
        "insert into contact_inquiry(id,email,phone,reason,message,created_at)"
            + " values(?,?,?,?,?,now())",
        id,
        r.email(),
        r.phone(),
        r.reason(),
        r.message());
    s.audit(null, "CONTACT_REQUEST", id.toString());
    return id;
  }
}
