package com.cms.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.entity.CmsContent;

public interface CmsContentRepository extends JpaRepository<CmsContent, UUID> {

  List<CmsContent> findByBasicInfo_SectionKeyOrderByBasicInfo_SortOrderAsc(String sectionKey);

  List<CmsContent> findByBasicInfo_SectionKeyAndBasicInfo_IsActiveTrueOrderByBasicInfo_SortOrderAsc(String sectionKey);

  List<CmsContent> findByBasicInfo_IsActiveTrue();
}
