package com.cms.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import com.cms.dto.DtoPage;
import com.cms.dto.DtoPageIU;
import com.cms.entity.Page;

@Mapper(componentModel = "spring")
public interface PageMapper {
  DtoPage toDtoPage(Page page);

  Page toPage(DtoPageIU dtoPageIU);

  void updatePageFromDto(DtoPageIU dtoPageIU, @MappingTarget Page page);
}
