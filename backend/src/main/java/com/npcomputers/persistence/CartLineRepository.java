package com.npcomputers.persistence;

import com.npcomputers.domain.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;

public interface CartLineRepository extends JpaRepository<CartLine, UUID> {
  List<CartLine> findByUserIdOrderByProductId(UUID userId);

  Optional<CartLine> findByUserIdAndProductId(UUID userId, UUID productId);

  void deleteByUserId(UUID userId);
}
