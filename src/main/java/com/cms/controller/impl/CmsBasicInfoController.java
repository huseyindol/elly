package com.cms.controller.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.DtoCmsBasicInfo;
import com.cms.dto.DtoCmsBasicInfoIU;
import com.cms.entity.CmsBasicInfo;
import com.cms.entity.RootEntityResponse;
import com.cms.mapper.CmsBasicInfoMapper;
import com.cms.service.ICmsBasicInfoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/basic-infos")
@RequiredArgsConstructor
public class CmsBasicInfoController extends BaseController {

  private final ICmsBasicInfoService cmsBasicInfoService;
  private final CmsBasicInfoMapper cmsBasicInfoMapper;

  @GetMapping("/{id}")
  public RootEntityResponse<DtoCmsBasicInfo> getBasicInfoById(@PathVariable UUID id) {
    CmsBasicInfo info = cmsBasicInfoService.getCmsBasicInfoById(id);
    DtoCmsBasicInfo dto = cmsBasicInfoMapper.toDtoCmsBasicInfo(info);
    return ok(dto);
  }

  @GetMapping("/list")
  public RootEntityResponse<List<DtoCmsBasicInfo>> getAllBasicInfos() {
    List<CmsBasicInfo> infos = cmsBasicInfoService.getAllCmsBasicInfos();
    List<DtoCmsBasicInfo> dtos = cmsBasicInfoMapper.toDtoCmsBasicInfoList(infos);
    return ok(dtos);
  }

  @GetMapping("/section/{sectionKey}")
  public RootEntityResponse<List<DtoCmsBasicInfo>> getBasicInfosBySectionKey(@PathVariable String sectionKey) {
    List<CmsBasicInfo> infos = cmsBasicInfoService.getActiveCmsBasicInfosBySectionKey(sectionKey);
    List<DtoCmsBasicInfo> dtos = cmsBasicInfoMapper.toDtoCmsBasicInfoList(infos);
    return ok(dtos);
  }

  @PostMapping
  public RootEntityResponse<DtoCmsBasicInfo> createBasicInfo(@Valid @RequestBody DtoCmsBasicInfoIU dtoIU) {
    CmsBasicInfo info = cmsBasicInfoMapper.toCmsBasicInfo(dtoIU);
    CmsBasicInfo savedInfo = cmsBasicInfoService.saveCmsBasicInfo(info);
    DtoCmsBasicInfo dto = cmsBasicInfoMapper.toDtoCmsBasicInfo(savedInfo);
    return ok(dto);
  }

  @PutMapping("/{id}")
  public RootEntityResponse<DtoCmsBasicInfo> updateBasicInfo(@PathVariable UUID id,
      @Valid @RequestBody DtoCmsBasicInfoIU dtoIU) {
    CmsBasicInfo info = cmsBasicInfoService.getCmsBasicInfoById(id);
    cmsBasicInfoMapper.updateCmsBasicInfoFromDto(dtoIU, info);
    CmsBasicInfo savedInfo = cmsBasicInfoService.saveCmsBasicInfo(info);
    DtoCmsBasicInfo dto = cmsBasicInfoMapper.toDtoCmsBasicInfo(savedInfo);
    return ok(dto);
  }

  @DeleteMapping("/{id}")
  public RootEntityResponse<Boolean> deleteBasicInfo(@PathVariable UUID id) {
    Boolean deleted = cmsBasicInfoService.deleteCmsBasicInfo(id);
    if (deleted) {
      return ok(deleted);
    }
    return error("CMS Basic Info not deleted");
  }
}
