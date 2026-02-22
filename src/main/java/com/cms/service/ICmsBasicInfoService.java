package com.cms.service;

import java.util.List;
import java.util.UUID;

import com.cms.entity.CmsBasicInfo;

public interface ICmsBasicInfoService {
  CmsBasicInfo getCmsBasicInfoById(UUID id);

  List<CmsBasicInfo> getAllCmsBasicInfos();

  List<CmsBasicInfo> getActiveCmsBasicInfosBySectionKey(String sectionKey);

  CmsBasicInfo saveCmsBasicInfo(CmsBasicInfo cmsBasicInfo);

  Boolean deleteCmsBasicInfo(UUID id);
}
