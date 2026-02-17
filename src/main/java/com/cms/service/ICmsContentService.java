package com.cms.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.cms.entity.CmsContent;

public interface ICmsContentService {

  CmsContent saveCmsContent(CmsContent cmsContent);

  CmsContent getCmsContentById(UUID id);

  Boolean deleteCmsContent(UUID id);

  List<CmsContent> getAllCmsContents();

  List<CmsContent> getCmsContentsBySectionKey(String sectionKey);

  List<CmsContent> getActiveCmsContentsBySectionKey(String sectionKey);

  Page<CmsContent> getAllCmsContentsPaged(Pageable pageable);
}
