package com.cms.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.cms.dto.DtoBannerSummary;
import com.cms.entity.Banner;
import com.cms.exception.ResourceNotFoundException;
import com.cms.repository.BannerRepository;
import com.cms.service.IBannerService;
import com.cms.service.IFileService;

@Service
public class BannerService implements IBannerService {

  @Autowired
  private BannerRepository bannerRepository;

  @Autowired
  private IFileService fileService;

  @Override
  @Transactional
  @CacheEvict(value = "banners", allEntries = true)
  public Banner saveBanner(Banner banner) {
    return bannerRepository.save(banner);
  }

  @Override
  @Transactional
  @CacheEvict(value = "banners", allEntries = true)
  public Banner saveBannerWithImage(Banner banner, MultipartFile imageFile) {
    if (imageFile != null && !imageFile.isEmpty()) {
      // Eski dosyayı sil (update işlemi için)
      if (banner.getId() != null) {
        try {
          Banner existingBanner = getBannerById(banner.getId());
          if (existingBanner.getImage() != null) {
            fileService.deleteImage(existingBanner.getImage());
          }
        } catch (ResourceNotFoundException e) {
          // Banner bulunamadıysa devam et (yeni kayıt olabilir)
        }
      }

      // Yeni dosyayı kaydet
      String imagePath = fileService.saveImage(imageFile, "banners");
      banner.setImage(imagePath);
    }
    return bannerRepository.save(banner);
  }

  @Override
  @Transactional
  @CacheEvict(value = "banners", allEntries = true)
  public Boolean deleteBanner(Long id) {
    if (bannerRepository.existsById(id)) {
      // Banner silinmeden önce image dosyasını da sil
      Banner banner = getBannerById(id);
      if (banner.getImage() != null) {
        fileService.deleteImage(banner.getImage());
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
    // TODO: Banner entity'sinde components ilişkisi tanımlandığında bu metod
    // güncellenecek
    // Şimdilik Component entity'sinden banners listesi alınabilir
    return java.util.Collections.emptyList();
  }

  @Override
  public List<Banner> getBannersByWidgetId(Long widgetId) {
    // TODO: Banner entity'sinde widgets ilişkisi tanımlandığında bu metod
    // güncellenecek
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
