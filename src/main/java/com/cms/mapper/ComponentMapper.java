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
import com.cms.entity.Component;
import com.cms.entity.Page;

@Mapper(componentModel = "spring")
public interface ComponentMapper {
  @Mapping(target = "pageIds", ignore = true)
  DtoComponent toDtoComponent(Component component);

  @AfterMapping
  default void mapPagesToPageIds(Component component, @MappingTarget DtoComponent dtoComponent) {
    if (component.getPages() != null && !component.getPages().isEmpty()) {
      List<Long> pageIds = component.getPages().stream()
          .map(Page::getId)
          .collect(Collectors.toList());
      dtoComponent.setPageIds(pageIds);
    } else {
      dtoComponent.setPageIds(new ArrayList<>());
    }
  }

  @Mapping(target = "pages", ignore = true)
  Component toComponent(DtoComponentIU dtoComponentIU);

  @Mapping(target = "pages", ignore = true)
  void updateComponentFromDto(DtoComponentIU dtoComponentIU, @MappingTarget Component component);
}
