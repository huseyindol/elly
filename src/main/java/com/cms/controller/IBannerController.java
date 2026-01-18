package com.cms.controller;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.cms.dto.DtoBanner;
import com.cms.dto.DtoBannerIU;
import com.cms.dto.DtoBannerSummary;
import com.cms.entity.RootEntityResponse;

public interface IBannerController {
  // Tek endpoint - hem dosya hem URL destekler
  RootEntityResponse<DtoBanner> createBanner(DtoBannerIU dtoBannerIU,
      MultipartFile desktopImage, MultipartFile tabletImage, MultipartFile mobileImage);

  RootEntityResponse<DtoBanner> updateBanner(Long id, DtoBannerIU dtoBannerIU,
      MultipartFile desktopImage, MultipartFile tabletImage, MultipartFile mobileImage);

  RootEntityResponse<Boolean> deleteBanner(Long id);

  RootEntityResponse<DtoBanner> getBannerById(Long id);

  RootEntityResponse<List<DtoBanner>> getAllBanners();

  RootEntityResponse<List<DtoBannerSummary>> getAllBannersWithSummary();
}
