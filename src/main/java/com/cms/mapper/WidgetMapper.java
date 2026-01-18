package com.cms.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.mapstruct.AfterMapping;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import com.cms.dto.DtoBanner;
import com.cms.dto.DtoPost;
import com.cms.dto.DtoWidget;
import com.cms.dto.DtoWidgetIU;
import com.cms.entity.Banner;
import com.cms.entity.Post;
import com.cms.entity.Widget;

@Mapper(componentModel = "spring")
public interface WidgetMapper {
  @Mapping(target = "banners", ignore = true)
  @Mapping(target = "posts", ignore = true)
  DtoWidget toDtoWidget(Widget widget);

  @Named("toDtoWidgetSimple")
  @Mapping(target = "banners", ignore = true)
  @Mapping(target = "posts", ignore = true)
  DtoWidget toDtoWidgetSimple(Widget widget);

  @AfterMapping
  default void mapWidgetFields(Widget widget, @MappingTarget DtoWidget dtoWidget) {
    // Map banners
    if (widget.getBanners() != null && !widget.getBanners().isEmpty()) {
      List<DtoBanner> banners = widget.getBanners().stream()
          .map(this::bannerToDtoBanner)
          .collect(Collectors.toList());
      dtoWidget.setBanners(banners);
    } else {
      dtoWidget.setBanners(new ArrayList<>());
    }

    // Map posts
    if (widget.getPosts() != null && !widget.getPosts().isEmpty()) {
      List<DtoPost> posts = widget.getPosts().stream()
          .map(this::postToDtoPost)
          .collect(Collectors.toList());
      dtoWidget.setPosts(posts);
    } else {
      dtoWidget.setPosts(new ArrayList<>());
    }
  }

  default DtoBanner bannerToDtoBanner(Banner banner) {
    if (banner == null)
      return null;
    DtoBanner dto = new DtoBanner();
    dto.setId(banner.getId());
    dto.setTitle(banner.getTitle());
    dto.setImages(banner.getImages());
    dto.setAltText(banner.getAltText());
    dto.setLink(banner.getLink());
    dto.setTarget(banner.getTarget());
    dto.setOrderIndex(banner.getOrderIndex());
    dto.setStatus(banner.getStatus());
    dto.setType(banner.getType());
    return dto;
  }

  default DtoPost postToDtoPost(Post post) {
    if (post == null)
      return null;
    DtoPost dto = new DtoPost();
    dto.setId(post.getId());
    dto.setTitle(post.getTitle());
    dto.setSlug(post.getSlug());
    dto.setContent(post.getContent());
    dto.setStatus(post.getStatus());
    return dto;
  }

  @Mapping(target = "banners", ignore = true)
  @Mapping(target = "posts", ignore = true)
  Widget toWidget(DtoWidgetIU dtoWidgetIU);

  @Mapping(target = "banners", ignore = true)
  @Mapping(target = "posts", ignore = true)
  void updateWidgetFromDto(DtoWidgetIU dtoWidgetIU, @MappingTarget Widget widget);

  List<DtoWidget> toDtoWidgetList(List<Widget> widgets);

  @IterableMapping(qualifiedByName = "toDtoWidgetSimple")
  List<DtoWidget> toDtoWidgetListSimple(List<Widget> widgets);
}
