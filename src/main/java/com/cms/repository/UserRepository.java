package com.cms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cms.entity.User;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UserRepository extends JpaRepository<User, Long> {

  /**
   * Aktif + verilen rollerden en az birine sahip kullanıcılar. WebRTC ring-all
   * (tenant destek araması) hedeflerini bulmak için: rol adları panel rolleri
   * (SUPER_ADMIN/ADMIN/EDITOR/VIEWER) verilir, TENANT hariç tutulmuş olur.
   * DISTINCT — bir kullanıcı birden çok eşleşen role sahip olabilir.
   */
  @Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE u.isActive = true AND r.name IN :roleNames")
  List<User> findActiveUsersByRoleNames(@Param("roleNames") Set<String> roleNames);
  Optional<User> findByEmail(String email);

  Optional<User> findByUsername(String username);

  Boolean existsByEmail(String email);

  Boolean existsByUsername(String username);

  Optional<User> findByProviderAndProviderId(String provider, String providerId);

  Optional<User> findByEmailAndProvider(String email, String provider);

  Optional<User> findByEmailVerificationToken(String token);

  // Login optimizasyonu: Sadece token_version ve updated_at güncelle
  @Modifying
  @Query("UPDATE User u SET u.tokenVersion = :tokenVersion, u.updatedAt = CURRENT_TIMESTAMP WHERE u.id = :userId")
  void updateTokenVersion(@Param("userId") Long userId, @Param("tokenVersion") Long tokenVersion);
}
