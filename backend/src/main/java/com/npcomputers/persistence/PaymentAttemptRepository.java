package com.npcomputers.persistence;

import com.npcomputers.domain.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, UUID> {
  Optional<PaymentAttempt> findByOrderId(UUID id);
}
