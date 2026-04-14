package com.cms.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.cms.entity.CmsContent;

public interface ICmsContentService {

  CmsContent createCmsContent(CmsContent content, UUID basicInfoId, com.cms.entity.CmsBasicInfo newBasicInfo);

  List<CmsContent> createBulkCmsContents(UUID basicInfoId, com.cms.entity.CmsBasicInfo newBasicInfo,
      List<CmsContent> contents);

  CmsContent updateCmsContent(UUID id, CmsContent content, UUID basicInfoId,
      com.cms.entity.CmsBasicInfo updateBasicInfo);

  CmsContent saveCmsContent(CmsContent cmsContent);

  CmsContent getCmsContentById(UUID id);

  Boolean deleteCmsContent(UUID id);

  List<CmsContent> getAllCmsContents();

  List<CmsContent> getCmsContentsBySectionKey(String sectionKey);

  List<CmsContent> getActiveCmsContentsBySectionKey(String sectionKey);

  Page<CmsContent> getAllCmsContentsPaged(Pageable pageable);

  List<String> getDistinctSectionKeys();
}
