package com.cms.controller.impl;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.cms.controller.IBannerController;
import com.cms.dto.DtoBanner;
import com.cms.dto.DtoBannerIU;
import com.cms.entity.Banner;
import com.cms.entity.RootEntityResponse;
import com.cms.mapper.BannerMapper;
import com.cms.service.IBannerService;
import com.cms.service.impl.BannerService;

@RestController
@RequestMapping("/api/v1/banners")
public class BannerController extends BaseController implements IBannerController {

  @Autowired
  private IBannerService bannerService;

  @Autowired
  private BannerMapper bannerMapper;

  @Override
  @PostMapping(consumes = "multipart/form-data")
  public RootEntityResponse<DtoBanner> createBanner(@ParameterObject @ModelAttribute DtoBannerIU dtoBannerIU,
      @RequestParam(value = "image", required = true) MultipartFile imageFile) {
    try {
      Banner banner = bannerMapper.toBanner(dtoBannerIU);
      Banner savedBanner = bannerService.saveBannerWithImage(banner, imageFile);
      DtoBanner dtoBanner = bannerMapper.toDtoBanner(savedBanner);
      return ok(dtoBanner);
    } catch (Exception e) {
      throw new RuntimeException("Banner not created" + e.getMessage());
    }
  }

  @Override
  @PutMapping(value = "/{id}", consumes = "multipart/form-data")
  public RootEntityResponse<DtoBanner> updateBanner(@PathVariable Long id,
      @ParameterObject @ModelAttribute DtoBannerIU dtoBannerIU,
      @RequestParam(value = "image", required = false) MultipartFile imageFile) {
    try {
      Banner banner = bannerService.getBannerById(id);
      bannerMapper.updateBannerFromDto(dtoBannerIU, banner);
      Banner savedBanner = ((BannerService) bannerService).saveBannerWithImage(banner, imageFile);
      DtoBanner dtoBanner = bannerMapper.toDtoBanner(savedBanner);
      return ok(dtoBanner);
    } catch (Exception e) {
      throw new RuntimeException("Banner not updated" + e.getMessage());
    }

  }

  @Override
  @DeleteMapping("/{id}")
  public RootEntityResponse<Boolean> deleteBanner(@PathVariable Long id) {
    Boolean deleted = bannerService.deleteBanner(id);
    if (deleted) {
      return ok(deleted);
    }
    return error("Banner not deleted");
  }

  @Override
  @GetMapping("/{id}")
  public RootEntityResponse<DtoBanner> getBannerById(@PathVariable Long id) {
    Banner banner = bannerService.getBannerById(id);
    DtoBanner dtoBanner = bannerMapper.toDtoBanner(banner);
    return ok(dtoBanner);
  }

}
