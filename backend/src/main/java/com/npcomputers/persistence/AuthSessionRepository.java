package com.npcomputers.persistence;

import com.npcomputers.domain.*;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface AuthSessionRepository extends JpaRepository<AuthSession, UUID> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select s from AuthSession s where s.id=:id")
  Optional<AuthSession> lock(@Param("id") UUID id);

  List<AuthSession> findByUserId(UUID userId);
}
