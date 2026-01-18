package com.cms.service.impl;

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.cms.dto.DtoBannerSummary;
import com.cms.entity.Banner;
import com.cms.entity.BannerImage;
import com.cms.exception.ResourceNotFoundException;
import com.cms.repository.BannerRepository;
import com.cms.service.IBannerService;
import com.cms.service.IFileService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BannerService implements IBannerService {

  private final BannerRepository bannerRepository;
  private final IFileService fileService;

  @Override
  @Transactional
  @CacheEvict(value = "banners", allEntries = true)
  public Banner saveBanner(Banner banner) {
    return bannerRepository.save(banner);
  }

  @Override
  @Transactional
  @CacheEvict(value = "banners", allEntries = true)
  public Banner saveBannerWithImages(Banner banner, MultipartFile desktopImage,
      MultipartFile tabletImage, MultipartFile mobileImage) {

    BannerImage bannerImage = new BannerImage();

    // Desktop görsel (zorunlu)
    if (desktopImage != null && !desktopImage.isEmpty()) {
      String desktopPath = fileService.saveImage(desktopImage, "banners/desktop");
      bannerImage.setDesktop(desktopPath);
    }

    // Tablet görsel (opsiyonel)
    if (tabletImage != null && !tabletImage.isEmpty()) {
      String tabletPath = fileService.saveImage(tabletImage, "banners/tablet");
      bannerImage.setTablet(tabletPath);
    }

    // Mobile görsel (opsiyonel)
    if (mobileImage != null && !mobileImage.isEmpty()) {
      String mobilePath = fileService.saveImage(mobileImage, "banners/mobile");
      bannerImage.setMobile(mobilePath);
    }

    banner.setImages(bannerImage);
    return bannerRepository.save(banner);
  }

  @Override
  @Transactional
  @CacheEvict(value = "banners", allEntries = true)
  public Banner updateBannerWithImages(Banner banner, MultipartFile desktopImage,
      MultipartFile tabletImage, MultipartFile mobileImage) {

    BannerImage currentImages = banner.getImages();
    if (currentImages == null) {
      currentImages = new BannerImage();
    }

    // Desktop görsel güncelle
    if (desktopImage != null && !desktopImage.isEmpty()) {
      // Eski görseli sil
      if (currentImages.getDesktop() != null) {
        fileService.deleteImage(currentImages.getDesktop());
      }
      String desktopPath = fileService.saveImage(desktopImage, "banners/desktop");
      currentImages.setDesktop(desktopPath);
    }

    // Tablet görsel güncelle
    if (tabletImage != null && !tabletImage.isEmpty()) {
      if (currentImages.getTablet() != null) {
        fileService.deleteImage(currentImages.getTablet());
      }
      String tabletPath = fileService.saveImage(tabletImage, "banners/tablet");
      currentImages.setTablet(tabletPath);
    }

    // Mobile görsel güncelle
    if (mobileImage != null && !mobileImage.isEmpty()) {
      if (currentImages.getMobile() != null) {
        fileService.deleteImage(currentImages.getMobile());
      }
      String mobilePath = fileService.saveImage(mobileImage, "banners/mobile");
      currentImages.setMobile(mobilePath);
    }

    banner.setImages(currentImages);
    return bannerRepository.save(banner);
  }

  @Override
  @Transactional
  @CacheEvict(value = "banners", allEntries = true)
  public Boolean deleteBanner(Long id) {
    if (bannerRepository.existsById(id)) {
      Banner banner = getBannerById(id);
      // Banner silinmeden önce tüm görselleri sil
      if (banner.getImages() != null) {
        BannerImage images = banner.getImages();
        if (images.getDesktop() != null) {
          fileService.deleteImage(images.getDesktop());
        }
        if (images.getTablet() != null) {
          fileService.deleteImage(images.getTablet());
        }
        if (images.getMobile() != null) {
          fileService.deleteImage(images.getMobile());
        }
      }
      bannerRepository.deleteById(id);
      return true;
    }
    return false;
  }

  @Override
  @Cacheable(value = "banners", key = "#id")
  public Banner getBannerById(Long id) {
    return bannerRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Banner", id));
  }

  @Override
  public List<Banner> getBannersByComponentId(Long componentId) {
    return java.util.Collections.emptyList();
  }

  @Override
  public List<Banner> getBannersByWidgetId(Long widgetId) {
    return java.util.Collections.emptyList();
  }

  @Override
  @Cacheable(value = "banners", key = "'getAllBanners'")
  public List<Banner> getAllBanners() {
    return bannerRepository.findAll();
  }

  @Override
  @Cacheable(value = "banners", key = "'getAllBannersWithSummary'")
  public List<DtoBannerSummary> getAllBannersWithSummary() {
    return bannerRepository.findAllWithSummary();
  }
}
