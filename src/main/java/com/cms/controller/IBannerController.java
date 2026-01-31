package com.cms.controller;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.cms.dto.DtoBanner;
import com.cms.dto.DtoBannerSummary;
import com.cms.dto.PagedResponse;
import com.cms.entity.RootEntityResponse;

public interface IBannerController {
  // multipart/form-data - data: JSON String, dosyalar: opsiyonel
  RootEntityResponse<DtoBanner> createBanner(String dataJson,
      MultipartFile desktopImage, MultipartFile tabletImage, MultipartFile mobileImage);

  RootEntityResponse<DtoBanner> updateBanner(Long id, String dataJson,
      MultipartFile desktopImage, MultipartFile tabletImage, MultipartFile mobileImage);

  RootEntityResponse<Boolean> deleteBanner(Long id);

  RootEntityResponse<DtoBanner> getBannerById(Long id);

  // List endpoints - reverted to flat list
  RootEntityResponse<List<DtoBanner>> getAllBanners();

  RootEntityResponse<List<DtoBannerSummary>> getAllBannersWithSummary();

  // Filter by subFolder endpoints
  RootEntityResponse<List<DtoBanner>> getBannersBySubFolder(String subFolder);

  RootEntityResponse<List<DtoBannerSummary>> getBannersSummaryBySubFolder(String subFolder);

  RootEntityResponse<List<String>> getAllSubFolders();

  // Paginated endpoints
  RootEntityResponse<PagedResponse<DtoBanner>> getAllBannersPaged(int page, int size, String sort);

  RootEntityResponse<PagedResponse<DtoBannerSummary>> getAllBannersWithSummaryPaged(int page, int size, String sort);
}
