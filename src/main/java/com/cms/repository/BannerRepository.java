package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.NonNull;

import com.cms.dto.DtoBannerSummary;
import com.cms.entity.Banner;

public interface BannerRepository extends JpaRepository<Banner, Long> {
  @Override
  @NonNull
  Optional<Banner> findById(@NonNull Long id);

  @Query("SELECT new com.cms.dto.DtoBannerSummary(b.id, b.title, b.orderIndex, b.status) FROM Banner b")
  List<DtoBannerSummary> findAllWithSummary();

}
