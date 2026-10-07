package com.npcomputers.persistence;

import com.npcomputers.domain.*;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ResetTokenRepository extends JpaRepository<ResetToken, UUID> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select r from ResetToken r where r.digest=:digest")
  Optional<ResetToken> lockDigest(@Param("digest") String digest);
}
