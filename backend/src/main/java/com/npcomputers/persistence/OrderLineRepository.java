package com.npcomputers.persistence;

import com.npcomputers.domain.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;

public interface OrderLineRepository extends JpaRepository<OrderLine, UUID> {
  List<OrderLine> findByOrderId(UUID id);
}
