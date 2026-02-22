package com.cms.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.cms.dto.DtoCmsContent;
import com.cms.dto.DtoCmsContentIU;
import com.cms.entity.CmsContent;

@Mapper(componentModel = "spring", uses = { CmsBasicInfoMapper.class })
public interface CmsContentMapper {

  DtoCmsContent toDtoCmsContent(CmsContent cmsContent);

  List<DtoCmsContent> toDtoCmsContentList(List<CmsContent> cmsContents);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "basicInfo", ignore = true)
  CmsContent toCmsContent(DtoCmsContentIU dtoCmsContentIU);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "basicInfo", ignore = true)
  void updateCmsContentFromDto(DtoCmsContentIU dtoCmsContentIU, @MappingTarget CmsContent cmsContent);
}
