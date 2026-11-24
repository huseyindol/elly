package com.cms.repository;

import com.cms.entity.RefreshToken;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
  @EntityGraph(attributePaths = { "user" })
  Optional<RefreshToken> findByToken(String token);

  @Query("SELECT rt FROM RefreshToken rt WHERE rt.user.id = :userId ORDER BY rt.expiryDate DESC")
  List<RefreshToken> findAllByUserIdOrderByExpiryDateDesc(@Param("userId") Long userId);

  @Modifying
  @Query("DELETE FROM RefreshToken rt WHERE rt.expiryDate < :now")
  void deleteExpiredTokens(@Param("now") Date now);

  @Modifying
  @Query("DELETE FROM RefreshToken rt WHERE rt.user.id = :userId AND (rt.isRevoked = true OR rt.expiryDate < :now)")
  void deleteExpiredOrRevokedByUserId(@Param("userId") Long userId, @Param("now") Date now);

  @Modifying
  @Query("DELETE FROM RefreshToken rt WHERE rt.token = :token AND rt.user.id = :userId")
  int deleteByTokenAndUserId(@Param("token") String token, @Param("userId") Long userId);

  @Modifying
  @Query("DELETE FROM RefreshToken rt WHERE rt.user.id = :userId")
  void deleteByUserId(@Param("userId") Long userId);
}
