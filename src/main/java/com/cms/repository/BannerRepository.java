package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;

import com.cms.dto.DtoBannerSummary;
import com.cms.entity.Banner;

public interface BannerRepository extends JpaRepository<Banner, Long> {
  @Override
  @NonNull
  Optional<Banner> findById(@NonNull Long id);

  @Query("SELECT new com.cms.dto.DtoBannerSummary(b.id, b.title, b.orderIndex, b.status, b.subFolder) FROM Banner b")
  List<DtoBannerSummary> findAllWithSummary();

  @Query("SELECT new com.cms.dto.DtoBannerSummary(b.id, b.title, b.orderIndex, b.status, b.subFolder) FROM Banner b WHERE b.subFolder = :subFolder")
  List<DtoBannerSummary> findSummaryBySubFolder(@Param("subFolder") String subFolder);

  @Query("SELECT new com.cms.dto.DtoBannerSummary(b.id, b.title, b.orderIndex, b.status, b.subFolder) FROM Banner b WHERE b.subFolder IS NULL OR b.subFolder = '' ORDER BY b.orderIndex ASC")
  List<DtoBannerSummary> findSummaryBySubFolderIsNullOrEmpty();

  @Query("SELECT DISTINCT b.subFolder FROM Banner b WHERE b.subFolder IS NOT NULL AND b.subFolder <> '' ORDER BY b.subFolder ASC")
  List<String> findDistinctSubFolders();

  List<Banner> findBySubFolder(String subFolder);

  @Query("SELECT b FROM Banner b WHERE b.subFolder IS NULL OR b.subFolder = ''")
  List<Banner> findBySubFolderIsNullOrEmpty();
}
