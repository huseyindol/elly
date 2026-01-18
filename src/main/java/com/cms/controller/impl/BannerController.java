package com.cms.controller.impl;

import java.util.List;

import org.springdoc.core.annotations.ParameterObject;
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
import com.cms.dto.DtoBannerSummary;
import com.cms.entity.Banner;
import com.cms.entity.BannerImage;
import com.cms.entity.RootEntityResponse;
import com.cms.exception.BadRequestException;
import com.cms.mapper.BannerMapper;
import com.cms.service.IBannerService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/banners")
@RequiredArgsConstructor
public class BannerController extends BaseController implements IBannerController {

  private final IBannerService bannerService;
  private final BannerMapper bannerMapper;

  /**
   * Banner oluştur - Tek endpoint
   * Dosya gönderilirse dosyayı yükler, gönderilmezse DTO'daki URL'leri kullanır
   */
  @Override
  @PostMapping(consumes = "multipart/form-data")
  public RootEntityResponse<DtoBanner> createBanner(
      @ParameterObject @ModelAttribute DtoBannerIU dtoBannerIU,
      @RequestParam(value = "desktop", required = false) MultipartFile desktopImage,
      @RequestParam(value = "tablet", required = false) MultipartFile tabletImage,
      @RequestParam(value = "mobile", required = false) MultipartFile mobileImage) {
    try {
      Banner banner = bannerMapper.toBanner(dtoBannerIU);

      // Dosya mı yoksa URL mi gönderildi kontrol et
      boolean hasFiles = isFileUploaded(desktopImage) || isFileUploaded(tabletImage)
          || isFileUploaded(mobileImage);

      if (hasFiles) {
        // Dosya yüklendi - dosyaları kaydet
        banner = bannerService.saveBannerWithImages(banner, desktopImage, tabletImage, mobileImage);
      } else {
        // URL gönderildi - DTO'daki URL'leri kullan
        if (dtoBannerIU.getImages() != null) {
          banner.setImages(dtoBannerIU.getImages());
        }
        banner = bannerService.saveBanner(banner);
      }

      DtoBanner dtoBanner = bannerMapper.toDtoBanner(banner);
      return ok(dtoBanner);
    } catch (Exception e) {
      throw new BadRequestException("Banner oluşturulamadı: " + e.getMessage());
    }
  }

  /**
   * Banner güncelle - Tek endpoint
   * Dosya gönderilirse dosyayı yükler, gönderilmezse DTO'daki URL'leri kullanır
   */
  @Override
  @PutMapping(value = "/{id}", consumes = "multipart/form-data")
  public RootEntityResponse<DtoBanner> updateBanner(
      @PathVariable Long id,
      @ParameterObject @ModelAttribute DtoBannerIU dtoBannerIU,
      @RequestParam(value = "desktop", required = false) MultipartFile desktopImage,
      @RequestParam(value = "tablet", required = false) MultipartFile tabletImage,
      @RequestParam(value = "mobile", required = false) MultipartFile mobileImage) {
    try {
      Banner banner = bannerService.getBannerById(id);
      bannerMapper.updateBannerFromDto(dtoBannerIU, banner);

      // Dosya mı yoksa URL mi gönderildi kontrol et
      boolean hasFiles = isFileUploaded(desktopImage) || isFileUploaded(tabletImage)
          || isFileUploaded(mobileImage);

      if (hasFiles) {
        // Dosya yüklendi - dosyaları kaydet
        banner = bannerService.updateBannerWithImages(banner, desktopImage, tabletImage, mobileImage);
      } else {
        // URL gönderildi - DTO'daki URL'leri kullan
        if (dtoBannerIU.getImages() != null) {
          banner.setImages(dtoBannerIU.getImages());
        }
        banner = bannerService.saveBanner(banner);
      }

      DtoBanner dtoBanner = bannerMapper.toDtoBanner(banner);
      return ok(dtoBanner);
    } catch (Exception e) {
      throw new BadRequestException("Banner güncellenemedi: " + e.getMessage());
    }
  }

  /**
   * Dosya yüklenmiş mi kontrol et
   */
  private boolean isFileUploaded(MultipartFile file) {
    return file != null && !file.isEmpty();
  }

  @Override
  @DeleteMapping("/{id}")
  public RootEntityResponse<Boolean> deleteBanner(@PathVariable Long id) {
    Boolean deleted = bannerService.deleteBanner(id);
    if (deleted) {
      return ok(deleted);
    }
    throw new BadRequestException("Banner silinemedi");
  }

  @Override
  @GetMapping("/{id}")
  public RootEntityResponse<DtoBanner> getBannerById(@PathVariable Long id) {
    Banner banner = bannerService.getBannerById(id);
    DtoBanner dtoBanner = bannerMapper.toDtoBanner(banner);
    return ok(dtoBanner);
  }

  @Override
  @GetMapping("/list")
  public RootEntityResponse<List<DtoBanner>> getAllBanners() {
    List<Banner> banners = bannerService.getAllBanners();
    List<DtoBanner> dtoBanners = bannerMapper.toDtoBannerList(banners);
    return ok(dtoBanners);
  }

  @Override
  @GetMapping("/list/summary")
  public RootEntityResponse<List<DtoBannerSummary>> getAllBannersWithSummary() {
    List<DtoBannerSummary> dtoBannerSummaries = bannerService.getAllBannersWithSummary();
    return ok(dtoBannerSummaries);
  }
}
