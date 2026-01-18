package com.cms.service;

import java.util.List;
import java.util.Map;

import org.springframework.web.multipart.MultipartFile;

import com.cms.dto.DtoBanner;
import com.cms.dto.DtoBannerSummary;
import com.cms.entity.Banner;

public interface IBannerService {
  Banner saveBanner(Banner banner);

  Banner saveBannerWithImages(Banner banner, MultipartFile desktopImage, MultipartFile tabletImage,
      MultipartFile mobileImage);

  Banner updateBannerWithImages(Banner banner, MultipartFile desktopImage, MultipartFile tabletImage,
      MultipartFile mobileImage);

  Boolean deleteBanner(Long id);

  Banner getBannerById(Long id);

  List<Banner> getBannersByComponentId(Long componentId);

  List<Banner> getBannersByWidgetId(Long widgetId);

  List<Banner> getAllBanners();

  List<DtoBannerSummary> getAllBannersWithSummary();

  // Grouped banners by subFolder
  Map<String, List<DtoBanner>> getGroupedBanners();

  Map<String, List<DtoBannerSummary>> getGroupedBannersWithSummary();

  // Filter by subFolder
  List<DtoBanner> getBannersBySubFolder(String subFolder);

  List<DtoBannerSummary> getBannersSummaryBySubFolder(String subFolder);

  List<String> getAllSubFolders();
}
