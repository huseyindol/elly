package com.cms.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import com.cms.dto.DtoBanner;
import com.cms.dto.DtoBannerIU;
import com.cms.entity.Banner;
import com.cms.entity.BannerImage;

@Mapper(componentModel = "spring")
public interface BannerMapper {
  DtoBanner toDtoBanner(Banner banner);

  Banner toBanner(DtoBannerIU dtoBannerIU);

  void updateBannerFromDto(DtoBannerIU dtoBannerIU, @MappingTarget Banner banner);

  List<DtoBanner> toDtoBannerList(List<Banner> banners);

  /**
   * Mevcut banner görselleriyle yeni görselleri birleştirir.
   * Yeni görsel null ise mevcut değer korunur.
   */
  default void mergeImages(Banner banner, BannerImage newImages) {
    BannerImage currentImages = banner.getImages();
    if (currentImages == null) {
      currentImages = new BannerImage();
    }

    // Desktop: yeni değer varsa güncelle, null ise mevcut değeri koru
    if (newImages.getDesktop() != null) {
      currentImages.setDesktop(newImages.getDesktop());
    }

    // Tablet: yeni değer varsa güncelle, null ise mevcut değeri koru
    if (newImages.getTablet() != null) {
      currentImages.setTablet(newImages.getTablet());
    }

    // Mobile: yeni değer varsa güncelle, null ise mevcut değeri koru
    if (newImages.getMobile() != null) {
      currentImages.setMobile(newImages.getMobile());
    }

    banner.setImages(currentImages);
  }
}
