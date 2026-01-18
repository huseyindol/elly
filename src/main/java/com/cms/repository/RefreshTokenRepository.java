package com.cms.repository;

import com.cms.entity.RefreshToken;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
  @EntityGraph(attributePaths = { "user" })
  Optional<RefreshToken> findByToken(String token);

  @EntityGraph(attributePaths = { "user" })
  Optional<RefreshToken> findByUserId(Long userId);

  // Login optimizasyonu: User zaten elimizde olduğunda gereksiz JOIN yapmamak
  // için
  @Query("SELECT rt FROM RefreshToken rt WHERE rt.user.id = :userId")
  Optional<RefreshToken> findByUserIdLight(@Param("userId") Long userId);

  @Modifying
  @Query("DELETE FROM RefreshToken rt WHERE rt.expiryDate < :now")
  void deleteExpiredTokens(@Param("now") Date now);

  @Modifying
  @Query("DELETE FROM RefreshToken rt WHERE rt.user.id = :userId")
  void deleteByUserId(@Param("userId") Long userId);

  // Login optimizasyonu: INSERT veya UPDATE tek sorguda (PostgreSQL native query)
  @Modifying
  @Query(value = """
      INSERT INTO elly.refresh_tokens (user_id, token, expiry_date, is_revoked, created_at, updated_at)
      VALUES (:userId, :token, :expiryDate, false, NOW(), NOW())
      ON CONFLICT (user_id) DO UPDATE SET
        token = :token,
        expiry_date = :expiryDate,
        is_revoked = false,
        updated_at = NOW()
      """, nativeQuery = true)
  void upsertRefreshToken(@Param("userId") Long userId, @Param("token") String token,
      @Param("expiryDate") Date expiryDate);
}
