package com.cms.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.cms.entity.Banner;

public interface IBannerService {
  Banner saveBanner(Banner banner);

  Banner saveBannerWithImage(Banner banner, MultipartFile imageFile);

  Boolean deleteBanner(Long id);

  Banner getBannerById(Long id);

  List<Banner> getBannersByComponentId(Long componentId);

  List<Banner> getBannersByWidgetId(Long widgetId);
}
