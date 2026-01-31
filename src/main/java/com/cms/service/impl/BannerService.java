package com.cms.service.impl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.cms.dto.DtoBanner;
import com.cms.dto.DtoBannerSummary;
import com.cms.entity.Banner;
import com.cms.entity.BannerImage;
import com.cms.exception.ResourceNotFoundException;
import com.cms.mapper.BannerMapper;
import com.cms.repository.BannerRepository;
import com.cms.service.IBannerService;
import com.cms.service.IFileService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BannerService implements IBannerService {

  private final BannerRepository bannerRepository;
  private final IFileService fileService;
  private final BannerMapper bannerMapper;

  /**
   * SubFolder'a göre dosya yolu oluşturur
   * subFolder null/empty ise: banners/desktop
   * subFolder varsa: banners/{subFolder}/desktop
   */
  private String buildImagePath(String subFolder, String deviceType) {
    if (subFolder == null || subFolder.trim().isEmpty()) {
      return "banners/" + deviceType;
    }
    return "banners/" + subFolder.trim() + "/" + deviceType;
  }

  /**
   * SubFolder'a göre gruplandırma key'i oluşturur
   * null/empty ise: "banner"
   * değilse: "banner/{subFolder}"
   */
  private String buildGroupKey(String subFolder) {
    if (subFolder == null || subFolder.trim().isEmpty()) {
      return "banner";
    }
    return "banner/" + subFolder.trim();
  }

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
    String subFolder = banner.getSubFolder();

    // Desktop görsel (zorunlu)
    if (desktopImage != null && !desktopImage.isEmpty()) {
      String desktopPath = fileService.saveImage(desktopImage, buildImagePath(subFolder, "desktop"));
      bannerImage.setDesktop(desktopPath);
    }

    // Tablet görsel (opsiyonel)
    if (tabletImage != null && !tabletImage.isEmpty()) {
      String tabletPath = fileService.saveImage(tabletImage, buildImagePath(subFolder, "tablet"));
      bannerImage.setTablet(tabletPath);
    }

    // Mobile görsel (opsiyonel)
    if (mobileImage != null && !mobileImage.isEmpty()) {
      String mobilePath = fileService.saveImage(mobileImage, buildImagePath(subFolder, "mobile"));
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
    String subFolder = banner.getSubFolder();

    // Desktop görsel güncelle
    if (desktopImage != null && !desktopImage.isEmpty()) {
      // Eski görseli sil
      if (currentImages.getDesktop() != null) {
        fileService.deleteImage(currentImages.getDesktop());
      }
      String desktopPath = fileService.saveImage(desktopImage, buildImagePath(subFolder, "desktop"));
      currentImages.setDesktop(desktopPath);
    }

    // Tablet görsel güncelle
    if (tabletImage != null && !tabletImage.isEmpty()) {
      if (currentImages.getTablet() != null) {
        fileService.deleteImage(currentImages.getTablet());
      }
      String tabletPath = fileService.saveImage(tabletImage, buildImagePath(subFolder, "tablet"));
      currentImages.setTablet(tabletPath);
    }

    // Mobile görsel güncelle
    if (mobileImage != null && !mobileImage.isEmpty()) {
      if (currentImages.getMobile() != null) {
        fileService.deleteImage(currentImages.getMobile());
      }
      String mobilePath = fileService.saveImage(mobileImage, buildImagePath(subFolder, "mobile"));
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

  @Override
  @Cacheable(value = "banners", key = "'getGroupedBanners'")
  public Map<String, List<DtoBanner>> getGroupedBanners() {
    List<Banner> banners = bannerRepository.findAll();

    // SubFolder'a göre grupla
    Map<String, List<DtoBanner>> grouped = banners.stream()
        .collect(Collectors.groupingBy(
            banner -> buildGroupKey(banner.getSubFolder()),
            LinkedHashMap::new,
            Collectors.mapping(bannerMapper::toDtoBanner, Collectors.toList())));

    return grouped;
  }

  @Override
  @Cacheable(value = "banners", key = "'getGroupedBannersWithSummary'")
  public Map<String, List<DtoBannerSummary>> getGroupedBannersWithSummary() {
    List<DtoBannerSummary> banners = bannerRepository.findAllWithSummary();

    // SubFolder'a göre grupla
    Map<String, List<DtoBannerSummary>> grouped = banners.stream()
        .collect(Collectors.groupingBy(
            banner -> buildGroupKey(banner.getSubFolder()),
            LinkedHashMap::new,
            Collectors.toList()));

    return grouped;
  }

  @Override
  @Cacheable(value = "banners", key = "'getBannersBySubFolder_' + #subFolder")
  public List<DtoBanner> getBannersBySubFolder(String subFolder) {
    List<Banner> banners;
    if (subFolder == null || subFolder.trim().isEmpty()) {
      banners = bannerRepository.findBySubFolderIsNullOrEmpty();
    } else {
      banners = bannerRepository.findBySubFolder(subFolder.trim());
    }
    return bannerMapper.toDtoBannerList(banners);
  }

  @Override
  @Cacheable(value = "banners", key = "'getBannersSummaryBySubFolder_' + #subFolder")
  public List<DtoBannerSummary> getBannersSummaryBySubFolder(String subFolder) {
    if (subFolder == null || subFolder.trim().isEmpty()) {
      return bannerRepository.findSummaryBySubFolderIsNullOrEmpty();
    }
    return bannerRepository.findSummaryBySubFolder(subFolder.trim());
  }

  @Override
  @Cacheable(value = "banners", key = "'getAllSubFolders'")
  public List<String> getAllSubFolders() {
    List<String> subFolders = bannerRepository.findDistinctSubFolders();
    // "banners" (default) klasörünü listeye ekle
    List<String> result = new java.util.ArrayList<>();
    result.add("banners");
    if (subFolders != null) {
      result.addAll(subFolders);
    }
    return result.stream().distinct().collect(Collectors.toList());
  }

  // Paginated methods
  @Override
  @Cacheable(value = "banners", key = "'getAllBannersPaged_' + #pageable.pageNumber + '_' + #pageable.pageSize + '_' + #pageable.sort.toString()")
  public org.springframework.data.domain.Page<Banner> getAllBannersPaged(
      org.springframework.data.domain.Pageable pageable) {
    return bannerRepository.findAll(pageable);
  }

  @Override
  @Cacheable(value = "banners", key = "'getAllBannersWithSummaryPaged_' + #pageable.pageNumber + '_' + #pageable.pageSize + '_' + #pageable.sort.toString()")
  public org.springframework.data.domain.Page<DtoBannerSummary> getAllBannersWithSummaryPaged(
      org.springframework.data.domain.Pageable pageable) {
    return bannerRepository.findAllWithSummaryPaged(pageable);
  }
}
