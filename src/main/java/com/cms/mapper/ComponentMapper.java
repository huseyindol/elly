package com.cms.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.cms.dto.DtoComponent;
import com.cms.dto.DtoComponentIU;
import com.cms.dto.DtoPageSummary;
import com.cms.entity.Component;
import com.cms.entity.Page;

@Mapper(componentModel = "spring")
public interface ComponentMapper {
  @Mapping(target = "pageIds", ignore = true)
  @Mapping(target = "pages", ignore = true)
  DtoComponent toDtoComponent(Component component);

  @AfterMapping
  default void mapPagesFields(Component component, @MappingTarget DtoComponent dtoComponent) {
    if (component.getPages() != null && !component.getPages().isEmpty()) {
      // pageIds
      List<Long> pageIds = component.getPages().stream()
          .map(Page::getId)
          .collect(Collectors.toList());
      dtoComponent.setPageIds(pageIds);

      // pages (DtoPageSummary)
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
  }

  @Mapping(target = "pages", ignore = true)
  Component toComponent(DtoComponentIU dtoComponentIU);

  @Mapping(target = "pages", ignore = true)
  void updateComponentFromDto(DtoComponentIU dtoComponentIU, @MappingTarget Component component);

  List<DtoComponent> toDtoComponentList(List<Component> components);
}
