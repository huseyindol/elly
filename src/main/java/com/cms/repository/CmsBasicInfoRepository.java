package com.cms.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cms.entity.CmsBasicInfo;

@Repository
public interface CmsBasicInfoRepository extends JpaRepository<CmsBasicInfo, UUID> {
  List<CmsBasicInfo> findByIsActiveTrue();

  List<CmsBasicInfo> findBySectionKeyOrderBySortOrderAsc(String sectionKey);

  List<CmsBasicInfo> findBySectionKeyAndIsActiveTrueOrderBySortOrderAsc(String sectionKey);
}
