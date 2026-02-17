package com.cms.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.entity.CmsContent;
import com.cms.exception.ResourceNotFoundException;
import com.cms.repository.CmsContentRepository;
import com.cms.service.ICmsContentService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CmsContentService implements ICmsContentService {

  private final CmsContentRepository cmsContentRepository;

  @Override
  @Transactional
  @CacheEvict(value = "cmsContents", allEntries = true)
  public CmsContent saveCmsContent(CmsContent cmsContent) {
    return cmsContentRepository.save(cmsContent);
  }

  @Override
  @Cacheable(value = "cmsContents", key = "#id")
  public CmsContent getCmsContentById(UUID id) {
    return cmsContentRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("CmsContent", id));
  }

  @Override
  @Transactional
  @CacheEvict(value = "cmsContents", allEntries = true)
  public Boolean deleteCmsContent(UUID id) {
    if (cmsContentRepository.existsById(id)) {
      cmsContentRepository.deleteById(id);
      return true;
    }
    return false;
  }

  @Override
  @Cacheable(value = "cmsContents", key = "'getAllCmsContents'")
  public List<CmsContent> getAllCmsContents() {
    return cmsContentRepository.findAll();
  }

  @Override
  @Cacheable(value = "cmsContents", key = "'section_' + #sectionKey")
  public List<CmsContent> getCmsContentsBySectionKey(String sectionKey) {
    return cmsContentRepository.findBySectionKeyOrderBySortOrderAsc(sectionKey);
  }

  @Override
  @Cacheable(value = "cmsContents", key = "'section_active_' + #sectionKey")
  public List<CmsContent> getActiveCmsContentsBySectionKey(String sectionKey) {
    return cmsContentRepository.findBySectionKeyAndIsActiveTrueOrderBySortOrderAsc(sectionKey);
  }

  @Override
  @Cacheable(value = "cmsContents", key = "'getAllCmsContentsPaged_' + #pageable.pageNumber + '_' + #pageable.pageSize + '_' + #pageable.sort.toString()")
  public Page<CmsContent> getAllCmsContentsPaged(Pageable pageable) {
    return cmsContentRepository.findAll(pageable);
  }
}
