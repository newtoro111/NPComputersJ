package com.npcomputers.service;

import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutboxWorker {
  private final Support s;
  private final boolean mail;

  public OutboxWorker(Support s, @Value("${app.mail-sink}") boolean mail) {
    this.s = s;
    this.mail = mail;
  }

  @Scheduled(fixedDelay = 3000)
  @Transactional
  public void dispatch() {
    var rows =
        s.db.queryForList(
            "select * from outbox where status='PENDING' and next_attempt<=now() order by"
                + " created_at limit 20 for update skip locked");
    for (var e : rows) {
      UUID id = (UUID) e.get("id");
      String kind = (String) e.get("kind"), payload = (String) e.get("payload");
      // Both adapters are local database stubs. External adapters must use claims and call outside
      // transactions.
      if (kind.equals("REPLENISHMENT"))
        s.db.update(
            "update replenishment_request set status='DISPATCHED',dispatched_at=now() where id=?"
                + " and status='PENDING'",
            UUID.fromString(payload));
      if (kind.equals("CONFIRMATION") && mail) {
        var o =
            s.db.queryForMap(
                "select order_number,contact_snapshot from customer_order where id=?",
                UUID.fromString(payload));
        var contact =
            s.decode(
                (String) o.get("contact_snapshot"), com.npcomputers.api.Contracts.Contact.class);
        s.db.update(
            "insert into mail_sink(id,recipient,subject,body,created_at) values(?,?,?,?,now()) on"
                + " conflict(id) do nothing",
            id,
            contact.email(),
            "Simulated confirmation " + o.get("order_number"),
            "No real email was sent. No funds were collected.");
      }
      s.db.update(
          "update outbox set status='COMPLETED',attempts=attempts+1,completed_at=now() where id=?",
          id);
    }
  }
}
