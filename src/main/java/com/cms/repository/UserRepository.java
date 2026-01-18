package com.cms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cms.entity.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByEmail(String email);

  Optional<User> findByUsername(String username);

  Boolean existsByEmail(String email);

  Boolean existsByUsername(String username);

  Optional<User> findByProviderAndProviderId(String provider, String providerId);

  Optional<User> findByEmailAndProvider(String email, String provider);

  // Login optimizasyonu: Sadece token_version ve updated_at güncelle
  @Modifying
  @Query("UPDATE User u SET u.tokenVersion = :tokenVersion, u.updatedAt = CURRENT_TIMESTAMP WHERE u.id = :userId")
  void updateTokenVersion(@Param("userId") Long userId, @Param("tokenVersion") Long tokenVersion);
}
