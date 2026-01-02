package com.cms.controller;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.cms.dto.DtoBanner;
import com.cms.dto.DtoBannerIU;
import com.cms.entity.RootEntityResponse;

public interface IBannerController {
  public RootEntityResponse<DtoBanner> createBanner(DtoBannerIU dtoBannerIU, MultipartFile imageFile);

  public RootEntityResponse<DtoBanner> updateBanner(Long id, DtoBannerIU dtoBannerIU, MultipartFile imageFile);

  public RootEntityResponse<Boolean> deleteBanner(Long id);

  public RootEntityResponse<DtoBanner> getBannerById(Long id);

  public RootEntityResponse<List<DtoBanner>> getAllBanners();
}
