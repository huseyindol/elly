package com.cms.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.cms.dto.DtoBanner;
import com.cms.dto.DtoBannerIU;
import com.cms.entity.Banner;

@Mapper(componentModel = "spring")
public interface BannerMapper {
  DtoBanner toDtoBanner(Banner banner);

  @Mapping(target = "image", ignore = true)
  Banner toBanner(DtoBannerIU dtoBannerIU);

  @Mapping(target = "image", ignore = true)
  void updateBannerFromDto(DtoBannerIU dtoBannerIU, @MappingTarget Banner banner);

  List<DtoBanner> toDtoBannerList(List<Banner> banners);
}
