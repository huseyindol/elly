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
import com.cms.entity.CmsBasicInfo;
import com.cms.service.ICmsBasicInfoService;

@Service
@RequiredArgsConstructor
public class CmsContentService implements ICmsContentService {

  private final CmsContentRepository cmsContentRepository;
  private final ICmsBasicInfoService cmsBasicInfoService;

  @Override
  @Transactional
  @CacheEvict(value = "cmsContents", allEntries = true)
  public CmsContent createCmsContent(CmsContent content, UUID basicInfoId, CmsBasicInfo newBasicInfo) {
    if (basicInfoId != null) {
      CmsBasicInfo basicInfo = cmsBasicInfoService.getCmsBasicInfoById(basicInfoId);
      content.setBasicInfo(basicInfo);
    } else if (newBasicInfo != null) {
      CmsBasicInfo basicInfo = cmsBasicInfoService.saveCmsBasicInfo(newBasicInfo);
      content.setBasicInfo(basicInfo);
    } else {
      throw new IllegalArgumentException("Either basicInfoId or basicInfo must be provided");
    }
    return cmsContentRepository.save(content);
  }

  @Override
  @Transactional
  @CacheEvict(value = "cmsContents", allEntries = true)
  public List<CmsContent> createBulkCmsContents(UUID basicInfoId, CmsBasicInfo newBasicInfo,
      List<CmsContent> contents) {
    CmsBasicInfo basicInfo;

    if (basicInfoId != null) {
      basicInfo = cmsBasicInfoService.getCmsBasicInfoById(basicInfoId);
    } else if (newBasicInfo != null) {
      basicInfo = cmsBasicInfoService.saveCmsBasicInfo(newBasicInfo);
    } else {
      throw new IllegalArgumentException("Either basicInfoId or basicInfo must be provided");
    }

    for (CmsContent content : contents) {
      content.setBasicInfo(basicInfo);
    }

    return cmsContentRepository.saveAll(contents);
  }

  @Override

  @Transactional
  @CacheEvict(value = "cmsContents", allEntries = true)
  public CmsContent updateCmsContent(UUID id, CmsContent contentUpdate, UUID basicInfoId,
      CmsBasicInfo updateBasicInfo) {
    CmsContent content = getCmsContentById(id);

    // Update fields except basicInfo
    content.setContentType(contentUpdate.getContentType());
    content.setMetadata(contentUpdate.getMetadata());

    if (basicInfoId != null) {
      CmsBasicInfo basicInfo = cmsBasicInfoService.getCmsBasicInfoById(basicInfoId);
      content.setBasicInfo(basicInfo);
    } else if (updateBasicInfo != null) {
      if (content.getBasicInfo() != null) {
        // Update existing Basic Info
        CmsBasicInfo existing = content.getBasicInfo();
        existing.setSectionKey(updateBasicInfo.getSectionKey());
        existing.setTitle(updateBasicInfo.getTitle());
        existing.setDescription(updateBasicInfo.getDescription());
        existing.setIsActive(updateBasicInfo.getIsActive());
        existing.setSortOrder(updateBasicInfo.getSortOrder());
        cmsBasicInfoService.saveCmsBasicInfo(existing);
      } else {
        // Create new
        CmsBasicInfo basicInfo = cmsBasicInfoService.saveCmsBasicInfo(updateBasicInfo);
        content.setBasicInfo(basicInfo);
      }
    }

    return cmsContentRepository.save(content);
  }

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
    return cmsContentRepository.findByBasicInfo_SectionKeyOrderByBasicInfo_SortOrderAsc(sectionKey);
  }

  @Override
  @Cacheable(value = "cmsContents", key = "'section_active_' + #sectionKey")
  public List<CmsContent> getActiveCmsContentsBySectionKey(String sectionKey) {
    return cmsContentRepository
        .findByBasicInfo_SectionKeyAndBasicInfo_IsActiveTrueOrderByBasicInfo_SortOrderAsc(sectionKey);
  }

  @Override
  @Cacheable(value = "cmsContents", key = "'getAllCmsContentsPaged_' + #pageable.pageNumber + '_' + #pageable.pageSize + '_' + #pageable.sort.toString()")
  public Page<CmsContent> getAllCmsContentsPaged(Pageable pageable) {
    return cmsContentRepository.findAll(pageable);
  }
}
