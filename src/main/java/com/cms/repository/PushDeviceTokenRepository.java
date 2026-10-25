package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cms.entity.PushDeviceToken;

public interface PushDeviceTokenRepository extends JpaRepository<PushDeviceToken, Long> {

  List<PushDeviceToken> findByUserId(Long userId);

  Optional<PushDeviceToken> findByUserIdAndToken(Long userId, String token);

  @Modifying
  @Query("DELETE FROM PushDeviceToken p WHERE p.userId = :userId AND p.token = :token")
  int deleteByUserIdAndToken(@Param("userId") Long userId, @Param("token") String token);
}
