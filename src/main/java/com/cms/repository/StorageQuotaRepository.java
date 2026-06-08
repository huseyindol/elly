package com.cms.repository;

import com.cms.entity.StorageQuota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StorageQuotaRepository extends JpaRepository<StorageQuota, String> {

  /** Atomik artış (eşzamanlı upload'lar için güvenli). Satır yoksa 0 döner. */
  @Modifying
  @Query("UPDATE StorageQuota s SET s.usedBytes = s.usedBytes + :delta WHERE s.tenantId = :tid")
  int addUsage(@Param("tid") String tenantId, @Param("delta") long delta);

  /** Atomik azalış, 0'ın altına düşmez. */
  @Modifying
  @Query("UPDATE StorageQuota s SET s.usedBytes = "
      + "CASE WHEN s.usedBytes - :delta < 0 THEN 0 ELSE s.usedBytes - :delta END "
      + "WHERE s.tenantId = :tid")
  int removeUsage(@Param("tid") String tenantId, @Param("delta") long delta);
}
