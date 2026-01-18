package com.cms.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.mapstruct.AfterMapping;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import com.cms.dto.DtoBanner;
import com.cms.dto.DtoComponent;
import com.cms.dto.DtoComponentIU;
import com.cms.dto.DtoPageSummary;
import com.cms.dto.DtoWidget;
import com.cms.entity.Banner;
import com.cms.entity.Component;
import com.cms.entity.Page;
import com.cms.entity.Widget;

@Mapper(componentModel = "spring", uses = { BannerMapper.class, WidgetMapper.class })
public interface ComponentMapper {
  @Mapping(target = "pageIds", ignore = true)
  @Mapping(target = "pages", ignore = true)
  @Mapping(target = "banners", ignore = true)
  @Mapping(target = "widgets", ignore = true)
  DtoComponent toDtoComponent(Component component);

  @Named("toDtoComponentSimple")
  @Mapping(target = "pageIds", ignore = true)
  @Mapping(target = "pages", ignore = true)
  @Mapping(target = "banners", ignore = true)
  @Mapping(target = "widgets", ignore = true)
  DtoComponent toDtoComponentSimple(Component component);

  @AfterMapping
  default void mapComponentFields(Component component, @MappingTarget DtoComponent dtoComponent) {
    // Map pages
    if (component.getPages() != null && !component.getPages().isEmpty()) {
      List<Long> pageIds = component.getPages().stream()
          .map(Page::getId)
          .collect(Collectors.toList());
      dtoComponent.setPageIds(pageIds);

      List<DtoPageSummary> pages = component.getPages().stream()
          .map(page -> {
            DtoPageSummary summary = new DtoPageSummary();
            summary.setId(page.getId());
            summary.setTitle(page.getTitle());
            summary.setSlug(page.getSlug());
            summary.setStatus(page.getStatus());
            return summary;
          })
          .collect(Collectors.toList());
      dtoComponent.setPages(pages);
    } else {
      dtoComponent.setPageIds(new ArrayList<>());
      dtoComponent.setPages(new ArrayList<>());
    }

    // Map banners
    if (component.getBanners() != null && !component.getBanners().isEmpty()) {
      List<DtoBanner> banners = component.getBanners().stream()
          .map(this::bannerToDtoBanner)
          .collect(Collectors.toList());
      dtoComponent.setBanners(banners);
    }

    // Map widgets
    if (component.getWidgets() != null && !component.getWidgets().isEmpty()) {
      List<DtoWidget> widgets = component.getWidgets().stream()
          .map(this::widgetToDtoWidget)
          .collect(Collectors.toList());
      dtoComponent.setWidgets(widgets);
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

  default DtoWidget widgetToDtoWidget(Widget widget) {
    if (widget == null)
      return null;
    DtoWidget dto = new DtoWidget();
    dto.setId(widget.getId());
    dto.setName(widget.getName());
    dto.setDescription(widget.getDescription());
    dto.setType(widget.getType());
    dto.setContent(widget.getContent());
    dto.setOrderIndex(widget.getOrderIndex());
    dto.setStatus(widget.getStatus());
    return dto;
  }

  @Mapping(target = "pages", ignore = true)
  @Mapping(target = "banners", ignore = true)
  @Mapping(target = "widgets", ignore = true)
  Component toComponent(DtoComponentIU dtoComponentIU);

  @Mapping(target = "pages", ignore = true)
  @Mapping(target = "banners", ignore = true)
  @Mapping(target = "widgets", ignore = true)
  void updateComponentFromDto(DtoComponentIU dtoComponentIU, @MappingTarget Component component);

  @Named("toDtoComponentList")
  default List<DtoComponent> toDtoComponentList(Set<Component> components) {
    if (components == null)
      return new ArrayList<>();
    return components.stream()
        .map(this::toDtoComponent)
        .collect(Collectors.toList());
  }

  @IterableMapping(qualifiedByName = "toDtoComponentSimple")
  List<DtoComponent> toDtoComponentListSimple(List<Component> components);
}
