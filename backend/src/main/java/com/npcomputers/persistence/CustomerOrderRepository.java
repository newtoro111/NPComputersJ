package com.npcomputers.persistence;

import com.npcomputers.domain.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, UUID> {
  Page<CustomerOrder> findByUserIdOrderByCreatedAtDesc(UUID id, Pageable pageable);
}
