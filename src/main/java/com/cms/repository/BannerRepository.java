package com.cms.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;

import com.cms.entity.Banner;

public interface BannerRepository extends JpaRepository<Banner, Long> {
  @Override
  @NonNull
  Optional<Banner> findById(@NonNull Long id);
}
