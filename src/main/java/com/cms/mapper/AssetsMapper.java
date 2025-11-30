package com.cms.mapper;

import org.mapstruct.Mapper;

import com.cms.dto.DtoAssets;
import com.cms.entity.Assets;

@Mapper(componentModel = "spring")
public interface AssetsMapper {
  DtoAssets toDtoAssets(Assets assets);

}
