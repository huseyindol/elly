package com.cms.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.cms.dto.DtoCmsBasicInfo;
import com.cms.dto.DtoCmsBasicInfoIU;
import com.cms.entity.CmsBasicInfo;

@Mapper(componentModel = "spring")
public interface CmsBasicInfoMapper {

  DtoCmsBasicInfo toDtoCmsBasicInfo(CmsBasicInfo cmsBasicInfo);

  List<DtoCmsBasicInfo> toDtoCmsBasicInfoList(List<CmsBasicInfo> cmsBasicInfos);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  CmsBasicInfo toCmsBasicInfo(DtoCmsBasicInfoIU dtoCmsBasicInfoIU);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  void updateCmsBasicInfoFromDto(DtoCmsBasicInfoIU dtoCmsBasicInfoIU, @MappingTarget CmsBasicInfo cmsBasicInfo);
}
