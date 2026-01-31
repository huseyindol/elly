package com.cms.controller.impl;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.cms.controller.IBannerController;
import com.cms.dto.DtoBanner;
import com.cms.dto.DtoBannerIU;
import com.cms.dto.DtoBannerSummary;
import com.cms.dto.PagedResponse;
import com.cms.entity.Banner;

import com.cms.entity.RootEntityResponse;
import com.cms.exception.BadRequestException;
import com.cms.mapper.BannerMapper;
import com.cms.service.IBannerService;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/banners")
@RequiredArgsConstructor
public class BannerController extends BaseController implements IBannerController {

  private final IBannerService bannerService;
  private final BannerMapper bannerMapper;
  private final ObjectMapper objectMapper;

  /**
   * Banner oluştur
   * multipart/form-data ile gönderim:
   * - data: JSON formatında DtoBannerIU (String olarak)
   * - desktop: opsiyonel dosya
   * - tablet: opsiyonel dosya
   * - mobile: opsiyonel dosya
   */
  @Override
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public RootEntityResponse<DtoBanner> createBanner(
      @RequestPart("data") String dataJson,
      @RequestPart(value = "desktop", required = false) MultipartFile desktopImage,
      @RequestPart(value = "tablet", required = false) MultipartFile tabletImage,
      @RequestPart(value = "mobile", required = false) MultipartFile mobileImage) {
    try {
      // JSON String'i DTO'ya parse et
      DtoBannerIU dtoBannerIU = objectMapper.readValue(dataJson, DtoBannerIU.class);
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
   * Banner güncelle
   * multipart/form-data ile gönderim:
   * - data: JSON formatında DtoBannerIU (String olarak)
   * - desktop: opsiyonel dosya
   * - tablet: opsiyonel dosya
   * - mobile: opsiyonel dosya
   */
  @Override
  @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public RootEntityResponse<DtoBanner> updateBanner(
      @PathVariable Long id,
      @RequestPart("data") String dataJson,
      @RequestPart(value = "desktop", required = false) MultipartFile desktopImage,
      @RequestPart(value = "tablet", required = false) MultipartFile tabletImage,
      @RequestPart(value = "mobile", required = false) MultipartFile mobileImage) {
    try {
      // JSON String'i DTO'ya parse et
      DtoBannerIU dtoBannerIU = objectMapper.readValue(dataJson, DtoBannerIU.class);
      Banner banner = bannerService.getBannerById(id);

      // Mevcut görselleri kaydet (mapper tarafından ezilmemesi için)
      var originalImages = banner.getImages();

      bannerMapper.updateBannerFromDto(dtoBannerIU, banner);

      // Dosya mı yoksa URL mi gönderildi kontrol et
      boolean hasFiles = isFileUploaded(desktopImage) || isFileUploaded(tabletImage)
          || isFileUploaded(mobileImage);

      if (hasFiles) {
        // Mevcut görselleri geri yükle (updateBannerWithImages doğru merge yapabilsin)
        banner.setImages(originalImages);
        // Dosya yüklendi - dosyaları kaydet (mevcut görseller korunur, sadece yeni
        // yüklenenler güncellenir)
        banner = bannerService.updateBannerWithImages(banner, desktopImage, tabletImage, mobileImage);
      } else {
        // URL gönderildi - DTO'daki null olmayan URL'leri kullan, null olanlar mevcut
        // değeri korur
        if (dtoBannerIU.getImages() != null) {
          bannerMapper.mergeImages(banner, dtoBannerIU.getImages());
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
  @GetMapping("/{id:\\d+}")
  public RootEntityResponse<DtoBanner> getBannerById(@PathVariable Long id) {
    Banner banner = bannerService.getBannerById(id);
    DtoBanner dtoBanner = bannerMapper.toDtoBanner(banner);
    return ok(dtoBanner);
  }

  /**
   * Tüm banner'ları düz liste olarak döndürür
   */
  @Override
  @GetMapping("/list")
  public RootEntityResponse<List<DtoBanner>> getAllBanners() {
    List<Banner> banners = bannerService.getAllBanners();
    List<DtoBanner> dtoBanners = bannerMapper.toDtoBannerList(banners);
    return ok(dtoBanners);
  }

  /**
   * Tüm banner özetlerini düz liste olarak döndürür
   */
  @Override
  @GetMapping("/list/summary")
  public RootEntityResponse<List<DtoBannerSummary>> getAllBannersWithSummary() {
    List<DtoBannerSummary> dtoBannerSummaries = bannerService.getAllBannersWithSummary();
    return ok(dtoBannerSummaries);
  }

  /**
   * Belirli bir subFolder'daki banner'ları döndürür
   */
  @Override
  @GetMapping("/list/{subFolder}")
  public RootEntityResponse<List<DtoBanner>> getBannersBySubFolder(@PathVariable String subFolder) {
    // subFolder boşluk ise null yap
    if (subFolder != null && subFolder.trim().isEmpty()) {
      subFolder = null;
    }
    List<DtoBanner> banners = bannerService.getBannersBySubFolder(subFolder);
    return ok(banners);
  }

  /**
   * Belirli bir subFolder'daki banner özetlerini döndürür
   */
  @Override
  @GetMapping("/list/summary/{subFolder}")
  public RootEntityResponse<List<DtoBannerSummary>> getBannersSummaryBySubFolder(@PathVariable String subFolder) {
    if (subFolder != null && subFolder.trim().isEmpty()) {
      subFolder = null;
    }
    List<DtoBannerSummary> banners = bannerService.getBannersSummaryBySubFolder(subFolder);
    return ok(banners);
  }

  /**
   * Tüm mevcut subFolder listesini döndürür
   */
  @Override
  @GetMapping("/sub-folders")
  public RootEntityResponse<List<String>> getAllSubFolders() {
    List<String> subFolders = bannerService.getAllSubFolders();
    return ok(subFolders);
  }

  // Paginated endpoints
  @Override
  @GetMapping("/list/paged")
  public RootEntityResponse<PagedResponse<DtoBanner>> getAllBannersPaged(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "id,asc") String sort) {
    Pageable pageable = createPageable(page, size, sort);
    Page<Banner> pageResult = bannerService.getAllBannersPaged(pageable);
    List<DtoBanner> dtoBanners = bannerMapper.toDtoBannerList(pageResult.getContent());
    return ok(PagedResponse.from(pageResult, dtoBanners));
  }

  @Override
  @GetMapping("/list/summary/paged")
  public RootEntityResponse<PagedResponse<DtoBannerSummary>> getAllBannersWithSummaryPaged(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "id,asc") String sort) {
    Pageable pageable = createPageable(page, size, sort);
    Page<DtoBannerSummary> pageResult = bannerService.getAllBannersWithSummaryPaged(pageable);
    return ok(PagedResponse.from(pageResult));
  }

  private Pageable createPageable(int page, int size, String sort) {
    String[] sortParams = sort.split(",");
    String sortField = sortParams[0];
    Sort.Direction direction = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("desc")
        ? Sort.Direction.DESC
        : Sort.Direction.ASC;
    return PageRequest.of(page, size, Sort.by(direction, sortField));
  }
}
