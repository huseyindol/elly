package com.cms.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.entity.CmsContent;

public interface CmsContentRepository extends JpaRepository<CmsContent, UUID> {

  List<CmsContent> findBySectionKeyOrderBySortOrderAsc(String sectionKey);

  List<CmsContent> findBySectionKeyAndIsActiveTrueOrderBySortOrderAsc(String sectionKey);

  List<CmsContent> findByIsActiveTrue();
}
