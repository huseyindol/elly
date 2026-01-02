package com.cms.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.cms.dto.DtoPage;
import com.cms.dto.DtoPageIU;
import com.cms.entity.Page;

@Mapper(componentModel = "spring", uses = ComponentMapper.class)
public interface PageMapper {
  DtoPage toDtoPage(Page page);

  @Mapping(target = "components", ignore = true)
  Page toPage(DtoPageIU dtoPageIU);

  @Mapping(target = "components", ignore = true)
  void updatePageFromDto(DtoPageIU dtoPageIU, @MappingTarget Page page);

  List<DtoPage> toDtoPageList(List<Page> pages);
}
