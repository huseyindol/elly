package com.cms.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.entity.CmsBasicInfo;
import com.cms.exception.ResourceNotFoundException;
import com.cms.repository.CmsBasicInfoRepository;
import com.cms.service.ICmsBasicInfoService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CmsBasicInfoService implements ICmsBasicInfoService {

  private final CmsBasicInfoRepository cmsBasicInfoRepository;

  @Override
  @Transactional(readOnly = true)
  public CmsBasicInfo getCmsBasicInfoById(UUID id) {
    return cmsBasicInfoRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("CMS Basic Info not found with id: " + id));
  }

  @Override
  @Transactional(readOnly = true)
  public List<CmsBasicInfo> getAllCmsBasicInfos() {
    return cmsBasicInfoRepository.findAll();
  }

  @Override
  @Transactional(readOnly = true)
  public List<CmsBasicInfo> getActiveCmsBasicInfosBySectionKey(String sectionKey) {
    return cmsBasicInfoRepository.findBySectionKeyAndIsActiveTrueOrderBySortOrderAsc(sectionKey);
  }

  @Override
  @Transactional
  public CmsBasicInfo saveCmsBasicInfo(CmsBasicInfo cmsBasicInfo) {
    log.info("Saving CMS Basic Info");
    return cmsBasicInfoRepository.save(cmsBasicInfo);
  }

  @Override
  @Transactional
  public Boolean deleteCmsBasicInfo(UUID id) {
    log.info("Deleting CMS Basic Info with id: {}", id);
    if (!cmsBasicInfoRepository.existsById(id)) {
      throw new ResourceNotFoundException("CMS Basic Info not found with id: " + id);
    }
    cmsBasicInfoRepository.deleteById(id);
    return true;
  }
}
